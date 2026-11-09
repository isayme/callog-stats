package org.isayme.callstats.util

object DurationFormatter {

    data class Parts(
        val hours: Long,
        val minutes: Long,
        val seconds: Long,
    )

    fun parts(totalSeconds: Long): Parts {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        return Parts(
            hours = safeSeconds / 3600,
            minutes = (safeSeconds % 3600) / 60,
            seconds = safeSeconds % 60,
        )
    }
}
