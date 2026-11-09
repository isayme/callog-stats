package org.isayme.callstats.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.isayme.callstats.call.CallMonitorService
import org.isayme.callstats.call.CallState

/**
 * Debug-only hook for adb-driven call state simulation. It is merged only into debug builds:
 *
 * adb shell am broadcast -a org.isayme.callstats.debug.SIMULATE_CALL_STATE \
 *   --es state RINGING --es number 13800138000
 */
class CallStateSimulatorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(EXTRA_STATE) ?: return
        if (CallState.fromTelephonyExtra(state) == null) return

        ContextCompat.startForegroundService(
            context,
            CallMonitorService.callStateIntent(
                context = context,
                state = state,
                number = intent.getStringExtra(EXTRA_NUMBER).orEmpty(),
            ),
        )
    }

    private companion object {
        const val EXTRA_STATE = "state"
        const val EXTRA_NUMBER = "number"
    }
}
