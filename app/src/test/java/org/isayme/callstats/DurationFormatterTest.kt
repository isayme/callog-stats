package org.isayme.callstats

import org.isayme.callstats.util.DurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {

    @Test
    fun `zero duration`() {
        assertEquals(DurationFormatter.Parts(0, 0, 0), DurationFormatter.parts(0))
    }

    @Test
    fun `negative duration is clamped`() {
        assertEquals(DurationFormatter.Parts(0, 0, 0), DurationFormatter.parts(-10))
    }

    @Test
    fun `seconds only`() {
        assertEquals(DurationFormatter.Parts(0, 0, 59), DurationFormatter.parts(59))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals(DurationFormatter.Parts(0, 2, 5), DurationFormatter.parts(125))
    }

    @Test
    fun `hours minutes and seconds`() {
        assertEquals(DurationFormatter.Parts(2, 2, 5), DurationFormatter.parts(7325))
    }
}
