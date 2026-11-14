package org.isayme.callstats.call

import java.time.Instant
import java.time.ZoneId

data class CallStatsRequest(
    val number: String,
    val nowMillis: Long,
    val lookbackMonths: Int,
    val duplicateWindowMinutes: Int,
    val matchMode: NumberMatchMode,
)

data class CallStats(
    val number: String,
    val hasIncomingEver: Boolean,
    val hasAnyRecordEver: Boolean,
    val incomingTotal: Int,
    val incomingAnswered: Int,
    val incomingMissed: Int,
    val incomingTalkSeconds: Long,
    val outgoingTotal: Int,
    val outgoingConnected: Int,
    val outgoingTalkSeconds: Long,
    val lastIncomingAt: Long?,
    val lastAnsweredAt: Long?,
    val lastAnsweredSeconds: Long?,
    val lastOutgoingAt: Long?,
    val lastOutgoingSeconds: Long?,
    val recentIncomingCount: Int,
    val windowStartMillis: Long,
) {
    val isFirstIncoming: Boolean get() = !hasIncomingEver
    val totalTalkSeconds: Long get() = incomingTalkSeconds + outgoingTalkSeconds
}

object CallStatsCalculator {

    fun calculate(entries: List<CallLogEntry>, request: CallStatsRequest): CallStats {
        val matchingEntries = entries.filter {
            PhoneNumberMatcher.matches(it.number, request.number, request.matchMode)
        }

        val hasIncomingEver = matchingEntries.any { it.isIncoming() }
        val windowStart = monthsBefore(request.nowMillis, request.lookbackMonths)
        val duplicateStart = request.nowMillis - request.duplicateWindowMinutes * 60_000L

        val incoming = mutableListOf<CallLogEntry>()
        val outgoing = mutableListOf<CallLogEntry>()
        var recentIncomingCount = 1 // The currently ringing call is not in CallLog yet.

        for (entry in matchingEntries) {
            if (entry.dateMillis < windowStart || entry.dateMillis > request.nowMillis) continue

            when {
                entry.isIncoming() -> {
                    incoming += entry
                    if (entry.dateMillis >= duplicateStart) recentIncomingCount++
                }

                entry.type == CallTypes.OUTGOING -> outgoing += entry
            }
        }

        val answered = incoming.filter { it.isAnsweredIncoming() }
        val missed = incoming.filterNot { it.isAnsweredIncoming() }
        val connectedOutgoing = outgoing.filter { it.durationSeconds > 0 }

        val lastAnswered = answered.maxByOrNull { it.dateMillis }
        val lastOutgoing = outgoing.maxByOrNull { it.dateMillis }

        return CallStats(
            number = request.number,
            hasIncomingEver = hasIncomingEver,
            hasAnyRecordEver = matchingEntries.isNotEmpty(),
            incomingTotal = incoming.size,
            incomingAnswered = answered.size,
            incomingMissed = missed.size,
            incomingTalkSeconds = answered.sumOf { it.durationSeconds.coerceAtLeast(0) },
            outgoingTotal = outgoing.size,
            outgoingConnected = connectedOutgoing.size,
            outgoingTalkSeconds = connectedOutgoing.sumOf { it.durationSeconds.coerceAtLeast(0) },
            lastIncomingAt = incoming.maxOfOrNull { it.dateMillis },
            lastAnsweredAt = lastAnswered?.dateMillis,
            lastAnsweredSeconds = lastAnswered?.durationSeconds,
            lastOutgoingAt = lastOutgoing?.dateMillis,
            lastOutgoingSeconds = lastOutgoing?.durationSeconds,
            recentIncomingCount = recentIncomingCount.coerceAtLeast(1),
            windowStartMillis = windowStart,
        )
    }

    /**
     * Every call-log type that represents an inbound call. Besides INCOMING and
     * MISSED this covers the variants OEMs use for calls that never reached the
     * user: REJECTED (MIUI records a caller hanging up during ringing as type 5),
     * BLOCKED, VOICEMAIL, and ANSWERED_EXTERNALLY (answered on another device).
     * Without them a number that only ever called unanswered would show 0 calls.
     */
    private fun CallLogEntry.isIncoming(): Boolean = when (type) {
        CallTypes.INCOMING,
        CallTypes.MISSED,
        CallTypes.VOICEMAIL,
        CallTypes.REJECTED,
        CallTypes.BLOCKED,
        CallTypes.ANSWERED_EXTERNALLY,
        -> true

        else -> false
    }

    /**
     * Some OEMs store a missed call as INCOMING with zero duration, so a
     * non-zero duration is required in addition to the type check.
     */
    private fun CallLogEntry.isAnsweredIncoming(): Boolean = when (type) {
        CallTypes.INCOMING -> durationSeconds > 0
        CallTypes.ANSWERED_EXTERNALLY -> true
        else -> false
    }

    private fun monthsBefore(nowMillis: Long, months: Int): Long =
        Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .minusMonths(months.toLong())
            .toInstant()
            .toEpochMilli()
}
