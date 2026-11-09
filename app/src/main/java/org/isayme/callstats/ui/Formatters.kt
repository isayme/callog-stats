package org.isayme.callstats.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.isayme.callstats.R
import org.isayme.callstats.call.PhoneNumberMatcher
import org.isayme.callstats.overlay.CallPhase
import org.isayme.callstats.util.DurationFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun durationText(totalSeconds: Long): String {
    val parts = DurationFormatter.parts(totalSeconds)
    return when {
        parts.hours > 0 -> stringResource(
            R.string.duration_hours_minutes,
            parts.hours,
            parts.minutes,
        )

        parts.minutes > 0 -> stringResource(
            R.string.duration_minutes_seconds,
            parts.minutes,
            parts.seconds,
        )

        else -> stringResource(R.string.duration_seconds, parts.seconds)
    }
}

@Composable
fun relativeTimeText(timestampMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val moment = Instant.ofEpochMilli(timestampMillis).atZone(zone)
    val now = Instant.now().atZone(zone)
    val date = moment.toLocalDate()

    val dayText = when (date) {
        now.toLocalDate() -> stringResource(R.string.today)
        now.toLocalDate().minusDays(1) -> stringResource(R.string.yesterday)
        else -> moment.format(dayFormatter)
    }
    return "$dayText ${moment.format(timeFormatter)}"
}

@Composable
fun phaseText(phase: CallPhase): String = when (phase) {
    CallPhase.INCOMING -> stringResource(R.string.overlay_phase_incoming)
    CallPhase.OUTGOING -> stringResource(R.string.overlay_phase_outgoing)
    CallPhase.IN_CALL -> stringResource(R.string.overlay_phase_in_call)
}

fun displayNumber(raw: String): String {
    val digits = PhoneNumberMatcher.digitsOnly(raw)
    return if (digits.length == 11 && digits.startsWith("1")) {
        "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
    } else {
        raw.trim()
    }
}
