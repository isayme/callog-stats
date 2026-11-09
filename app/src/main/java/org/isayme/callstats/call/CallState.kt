package org.isayme.callstats.call

import android.telephony.TelephonyManager

enum class CallState {
    IDLE,
    RINGING,
    OFFHOOK,
    ;

    companion object {
        fun fromTelephonyExtra(value: String?): CallState? = when (value) {
            TelephonyManager.EXTRA_STATE_IDLE -> IDLE
            TelephonyManager.EXTRA_STATE_RINGING -> RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> OFFHOOK
            else -> null
        }
    }
}
