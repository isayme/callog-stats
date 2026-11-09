package org.isayme.callstats

import org.isayme.callstats.call.CallLogEntry
import org.isayme.callstats.call.CallStatsCalculator
import org.isayme.callstats.call.CallStatsRequest
import org.isayme.callstats.call.CallTypes
import org.isayme.callstats.call.NumberMatchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallStatsCalculatorTest {

    private val now = 1_700_000_000_000L
    private val number = "13800138000"

    @Test
    fun `no records means first incoming`() {
        val stats = calculate(emptyList())

        assertTrue(stats.isFirstIncoming)
        assertFalse(stats.hasAnyRecordEver)
        assertEquals(0, stats.incomingTotal)
        assertEquals(0, stats.outgoingTotal)
        assertEquals(1, stats.recentIncomingCount)
    }

    @Test
    fun `incoming calls are split into answered and missed`() {
        val stats = calculate(
            listOf(
                entry(CallTypes.INCOMING, minutesAgo = 30, durationSeconds = 120),
                entry(CallTypes.INCOMING, minutesAgo = 60, durationSeconds = 30),
                entry(CallTypes.MISSED, minutesAgo = 90, durationSeconds = 0),
            ),
        )

        assertFalse(stats.isFirstIncoming)
        assertEquals(3, stats.incomingTotal)
        assertEquals(2, stats.incomingAnswered)
        assertEquals(1, stats.incomingMissed)
        assertEquals(150L, stats.incomingTalkSeconds)
        assertEquals(now - 30 * 60_000L, stats.lastAnsweredAt)
        assertEquals(120L, stats.lastAnsweredSeconds)
    }

    @Test
    fun `outgoing calls count only connected ones as successful`() {
        val stats = calculate(
            listOf(
                entry(CallTypes.OUTGOING, minutesAgo = 20, durationSeconds = 65),
                entry(CallTypes.OUTGOING, minutesAgo = 40, durationSeconds = 0),
            ),
        )

        assertEquals(2, stats.outgoingTotal)
        assertEquals(1, stats.outgoingConnected)
        assertEquals(65L, stats.outgoingTalkSeconds)
    }

    @Test
    fun `out of window calls affect first incoming but not statistics`() {
        val stats = calculate(
            listOf(
                entry(CallTypes.INCOMING, daysAgo = 300, durationSeconds = 60),
            ),
        )

        assertFalse(stats.isFirstIncoming)
        assertEquals(0, stats.incomingTotal)
        assertTrue(stats.hasAnyRecordEver)
    }

    @Test
    fun `repeat calls within window include the current call`() {
        val stats = calculate(
            listOf(
                entry(CallTypes.MISSED, minutesAgo = 2, durationSeconds = 0),
                entry(CallTypes.INCOMING, minutesAgo = 6, durationSeconds = 10),
                entry(CallTypes.INCOMING, minutesAgo = 30, durationSeconds = 20),
            ),
        )

        assertEquals(3, stats.recentIncomingCount)
    }

    @Test
    fun `repeat calls outside window are not counted`() {
        val stats = calculate(
            listOf(
                entry(CallTypes.INCOMING, minutesAgo = 30, durationSeconds = 10),
            ),
        )

        assertEquals(1, stats.recentIncomingCount)
    }

    @Test
    fun `normalized matching finds prefixed numbers`() {
        val stats = CallStatsCalculator.calculate(
            entries = listOf(
                CallLogEntry(
                    number = "+8613800138000",
                    type = CallTypes.INCOMING,
                    dateMillis = now - 60_000,
                    durationSeconds = 42,
                ),
            ),
            request = request(NumberMatchMode.NORMALIZED),
        )

        assertEquals(1, stats.incomingTotal)
        assertEquals(42L, stats.incomingTalkSeconds)
    }

    @Test
    fun `exact matching ignores differently prefixed numbers`() {
        val stats = CallStatsCalculator.calculate(
            entries = listOf(
                CallLogEntry(
                    number = "+8613800138000",
                    type = CallTypes.INCOMING,
                    dateMillis = now - 60_000,
                    durationSeconds = 42,
                ),
            ),
            request = request(NumberMatchMode.EXACT),
        )

        assertEquals(0, stats.incomingTotal)
        assertTrue(stats.isFirstIncoming)
    }

    private fun calculate(entries: List<CallLogEntry>) =
        CallStatsCalculator.calculate(entries, request(NumberMatchMode.NORMALIZED))

    private fun request(mode: NumberMatchMode) = CallStatsRequest(
        number = number,
        nowMillis = now,
        lookbackMonths = 6,
        duplicateWindowMinutes = 10,
        matchMode = mode,
    )

    private fun entry(
        type: Int,
        minutesAgo: Int = 0,
        daysAgo: Int = 0,
        durationSeconds: Long,
    ) = CallLogEntry(
        number = number,
        type = type,
        dateMillis = now - minutesAgo * 60_000L - daysAgo * 24 * 60 * 60 * 1000L,
        durationSeconds = durationSeconds,
    )
}
