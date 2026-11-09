package org.isayme.callstats.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.isayme.callstats.permissions.PermissionState

class PhoneStateReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION") // EXTRA_INCOMING_NUMBER is still the only call-number source in broadcasts.
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!PermissionState.current(context).callAccessGranted) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        Log.d(TAG, "Phone state=$state number=${if (number.isEmpty()) "empty" else "present"}")

        try {
            ContextCompat.startForegroundService(
                context,
                CallMonitorService.callStateIntent(context, state, number),
            )
        } catch (error: Exception) {
            // Background start restrictions may reject this when the service is not running.
            // The persistent foreground service remains the primary detection path.
            Log.w(TAG, "Unable to forward phone state", error)
        }
    }

    private companion object {
        const val TAG = "PhoneStateReceiver"
    }
}
