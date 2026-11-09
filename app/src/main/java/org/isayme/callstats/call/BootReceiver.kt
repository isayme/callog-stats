package org.isayme.callstats.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.isayme.callstats.permissions.PermissionState
import org.isayme.callstats.settings.SettingsRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settings.first()
                if (!settings.monitoringEnabled) return@launch
                if (!PermissionState.current(context).readyForMonitoring) return@launch

                CallMonitorService.start(context)
            } catch (error: Exception) {
                Log.w(TAG, "Unable to restore call monitoring after boot", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
