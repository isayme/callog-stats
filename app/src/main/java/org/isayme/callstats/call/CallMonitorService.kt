package org.isayme.callstats.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isayme.callstats.MainActivity
import org.isayme.callstats.R
import org.isayme.callstats.overlay.CallOverlayController
import org.isayme.callstats.overlay.CallPhase
import org.isayme.callstats.overlay.OverlayAccessibilityService
import org.isayme.callstats.overlay.OverlayUiState
import org.isayme.callstats.permissions.PermissionState
import org.isayme.callstats.settings.AppSettings
import org.isayme.callstats.settings.SettingsRepository

class CallMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var overlayController: CallOverlayController
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var settingsRepository: SettingsRepository

    private var settings = AppSettings()
    private var lastState = CallState.IDLE
    private var activeCall: ActiveCall? = null
    private var overlayVisibleForCall = false
    private var callSequence = 0L

    private var hideJob: Job? = null
    private var collapseJob: Job? = null
    private var previewJob: Job? = null

    /**
     * Android 10 (and some OEM builds) strips the incoming number from the
     * PHONE_STATE broadcast unless the receiver holds a privileged permission.
     * The deprecated PhoneStateListener callback still exposes the number to
     * apps with READ_CALL_LOG, so both paths feed the same state machine and the
     * first non-empty number wins.
     */
    @Suppress("DEPRECATION")
    private lateinit var phoneStateListener: PhoneStateListener

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayController = CallOverlayController(this)
        callLogRepository = CallLogRepository(contentResolver)
        settingsRepository = SettingsRepository(applicationContext)

        // Constructed here (not in a field initializer) because the service base
        // context is only attached after construction.
        phoneStateListener = object : PhoneStateListener(ContextCompat.getMainExecutor(this)) {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                val callState = when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
                    TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
                    TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
                    else -> return
                }
                previewJob?.cancel()
                handleCallState(callState, phoneNumber)
            }
        }

        runCatching {
            getSystemService(TelephonyManager::class.java)
                ?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }.onFailure { error ->
            Log.w(TAG, "Unable to listen for phone state", error)
        }

        serviceScope.launch {
            settingsRepository.settings.collect { updated ->
                settings = updated
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()

        when (intent?.action) {
            ACTION_STOP -> serviceScope.launch {
                settingsRepository.setMonitoringEnabled(false)
                stopMonitoring()
            }

            ACTION_PREVIEW -> showPreview()
            ACTION_CALL_STATE -> {
                previewJob?.cancel()
                handleCallState(
                    rawState = intent.getStringExtra(EXTRA_CALL_STATE),
                    number = intent.getStringExtra(EXTRA_NUMBER),
                )
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideJob?.cancel()
        collapseJob?.cancel()
        previewJob?.cancel()
        runCatching {
            if (::phoneStateListener.isInitialized) {
                getSystemService(TelephonyManager::class.java)
                    ?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
        }
        overlayController.hide()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleCallState(rawState: String?, number: String?) {
        val state = CallState.fromTelephonyExtra(rawState) ?: return
        handleCallState(state, number)
    }

    private fun handleCallState(state: CallState, number: String?) {
        if (!PermissionState.current(this).callAccessGranted) return

        val candidate = number?.trim().orEmpty()
        hideJob?.cancel()

        when (state) {
            CallState.RINGING -> handleRinging(candidate)
            CallState.OFFHOOK -> handleOffhook(candidate)
            CallState.IDLE -> handleIdle()
        }
    }

    private fun handleRinging(candidate: String) {
        val current = activeCall
        if (lastState == CallState.RINGING && current != null) {
            if (candidate.isNotEmpty() && current.number.isNotEmpty() && candidate != current.number) {
                // Call waiting: treat the new incoming call as the active call.
                startNewCall(candidate, CallPhase.INCOMING, showOverlay = true)
            } else if (candidate.isNotEmpty() && current.number.isEmpty()) {
                activeCall = current.copy(number = candidate)
                showStats(callSequence, candidate, current.phase)
            }
            lastState = CallState.RINGING
            return
        }

        startNewCall(candidate, CallPhase.INCOMING, showOverlay = true)
        lastState = CallState.RINGING
    }

    private fun handleOffhook(candidate: String) {
        when (lastState) {
            CallState.RINGING -> {
                val current = activeCall
                if (current == null) {
                    startNewCall(candidate, CallPhase.IN_CALL, showOverlay = true)
                } else {
                    val number = candidate.ifEmpty { current.number }
                    activeCall = current.copy(number = number, phase = CallPhase.IN_CALL)
                    if (overlayVisibleForCall) {
                        overlayController.update { it.copy(phase = CallPhase.IN_CALL) }
                    } else if (number.isNotEmpty()) {
                        showStats(callSequence, number, CallPhase.IN_CALL)
                    }
                }
                scheduleAutoCollapse()
            }

            CallState.OFFHOOK -> {
                val current = activeCall
                if (current != null && candidate.isNotEmpty() && current.number.isEmpty()) {
                    activeCall = current.copy(number = candidate)
                    if (!overlayVisibleForCall && settings.showOnOutgoing) {
                        showStats(callSequence, candidate, current.phase)
                        scheduleAutoCollapse()
                    }
                }
            }

            CallState.IDLE -> {
                startNewCall(candidate, CallPhase.OUTGOING, showOverlay = settings.showOnOutgoing)
                scheduleAutoCollapse()
            }
        }
        lastState = CallState.OFFHOOK
    }

    private fun handleIdle() {
        lastState = CallState.IDLE
        activeCall = null
        overlayVisibleForCall = false
        callSequence++
        collapseJob?.cancel()
        hideJob?.cancel()
        hideJob = serviceScope.launch {
            delay(HIDE_DELAY_MS)
            overlayController.hide()
        }
    }

    private fun startNewCall(number: String, phase: CallPhase, showOverlay: Boolean) {
        callSequence++
        collapseJob?.cancel()
        previewJob?.cancel()
        overlayVisibleForCall = false
        activeCall = ActiveCall(number = number, phase = phase)
        overlayController.hide()
        if (showOverlay && number.isNotEmpty()) {
            showStats(callSequence, number, phase)
        }
    }

    private fun showStats(sequence: Long, number: String, phase: CallPhase) {
        if (!canShowOverlay()) return

        val snapshot = settings
        overlayVisibleForCall = true
        overlayController.show(
            OverlayUiState(
                number = number,
                phase = phase,
                loading = true,
                duplicateWindowMinutes = snapshot.duplicateWindowMinutes,
            ),
        )

        serviceScope.launch {
            val matchMode = if (snapshot.normalizeNumbers) {
                NumberMatchMode.NORMALIZED
            } else {
                NumberMatchMode.EXACT
            }
            val result = runCatching {
                val entries = withContext(Dispatchers.IO) {
                    callLogRepository.loadMatchingCalls(number, matchMode)
                }
                withContext(Dispatchers.Default) {
                    CallStatsCalculator.calculate(
                        entries = entries,
                        request = CallStatsRequest(
                            number = number,
                            nowMillis = System.currentTimeMillis(),
                            lookbackMonths = snapshot.lookbackMonths,
                            duplicateWindowMinutes = snapshot.duplicateWindowMinutes,
                            matchMode = matchMode,
                        ),
                    )
                }
            }
            if (sequence != callSequence) return@launch

            result.onSuccess { stats ->
                overlayController.update {
                    it.copy(stats = stats, loading = false, loadFailed = false)
                }
            }.onFailure { error ->
                Log.w(TAG, "Unable to calculate call stats", error)
                overlayController.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    private fun scheduleAutoCollapse() {
        collapseJob?.cancel()
        collapseJob = serviceScope.launch {
            delay(AUTO_COLLAPSE_DELAY_MS)
            overlayController.setCollapsed(true)
        }
    }

    private fun showPreview() {
        if (!canShowOverlay()) return

        hideJob?.cancel()
        collapseJob?.cancel()
        previewJob?.cancel()

        val now = System.currentTimeMillis()
        val demoStats = CallStats(
            number = PREVIEW_NUMBER,
            hasIncomingEver = true,
            hasAnyRecordEver = true,
            incomingTotal = 5,
            incomingAnswered = 3,
            incomingMissed = 2,
            incomingTalkSeconds = 793,
            outgoingTotal = 2,
            outgoingConnected = 1,
            outgoingTalkSeconds = 65,
            lastIncomingAt = now - 60 * 60 * 1000L,
            lastAnsweredAt = now - 60 * 60 * 1000L,
            lastAnsweredSeconds = 132,
            lastOutgoingAt = now - 24 * 60 * 60 * 1000L,
            lastOutgoingSeconds = 65,
            recentIncomingCount = 2,
            windowStartMillis = now - 180L * 24 * 60 * 60 * 1000,
        )

        overlayController.show(
            OverlayUiState(
                number = PREVIEW_NUMBER,
                phase = CallPhase.INCOMING,
                stats = demoStats,
                loading = false,
                preview = true,
                duplicateWindowMinutes = settings.duplicateWindowMinutes,
            ),
        )

        previewJob = serviceScope.launch {
            delay(PREVIEW_DURATION_MS)
            overlayController.hide()
            if (!settings.monitoringEnabled) {
                stopMonitoring()
            }
        }
    }

    /**
     * Accessibility overlays do not need the SYSTEM_ALERT_WINDOW app op, so the
     * accessibility service is an independent, lock-screen-capable path.
     */
    private fun canShowOverlay(): Boolean =
        Settings.canDrawOverlays(this) || OverlayAccessibilityService.isConnected()

    private fun stopMonitoring() {        hideJob?.cancel()
        collapseJob?.cancel()
        previewJob?.cancel()
        overlayController.hide()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CallMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call_stats)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_stop),
                stopIntent,
            )
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private data class ActiveCall(
        val number: String,
        val phase: CallPhase,
    )

    companion object {
        private const val TAG = "CallMonitorService"
        private const val CHANNEL_ID = "call_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val AUTO_COLLAPSE_DELAY_MS = 4_000L
        private const val HIDE_DELAY_MS = 600L
        private const val PREVIEW_DURATION_MS = 12_000L
        private const val PREVIEW_NUMBER = "13800138000"

        private const val ACTION_START = "org.isayme.callstats.action.START"
        private const val ACTION_STOP = "org.isayme.callstats.action.STOP"
        private const val ACTION_PREVIEW = "org.isayme.callstats.action.PREVIEW"
        private const val ACTION_CALL_STATE = "org.isayme.callstats.action.CALL_STATE"
        private const val EXTRA_CALL_STATE = "extra_call_state"
        private const val EXTRA_NUMBER = "extra_number"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CallMonitorService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallMonitorService::class.java))
        }

        fun preview(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CallMonitorService::class.java).setAction(ACTION_PREVIEW),
            )
        }

        fun callStateIntent(context: Context, state: String, number: String): Intent =
            Intent(context, CallMonitorService::class.java)
                .setAction(ACTION_CALL_STATE)
                .putExtra(EXTRA_CALL_STATE, state)
                .putExtra(EXTRA_NUMBER, number)
    }
}
