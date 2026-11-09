package org.isayme.callstats.settings

data class AppSettings(
    val monitoringEnabled: Boolean = false,
    val duplicateWindowMinutes: Int = DEFAULT_DUPLICATE_WINDOW_MINUTES,
    val lookbackMonths: Int = DEFAULT_LOOKBACK_MONTHS,
    val normalizeNumbers: Boolean = true,
    val showOnOutgoing: Boolean = true,
) {
    companion object {
        const val DEFAULT_DUPLICATE_WINDOW_MINUTES = 10
        const val DEFAULT_LOOKBACK_MONTHS = 6
        val DUPLICATE_WINDOW_RANGE = 1..60
        val LOOKBACK_MONTHS_RANGE = 1..24
    }
}
