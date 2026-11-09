package org.isayme.callstats

import org.isayme.callstats.call.NumberMatchMode
import org.isayme.callstats.call.PhoneNumberMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberMatcherTest {

    @Test
    fun `digits only removes separators`() {
        assertEquals("13800138000", PhoneNumberMatcher.digitsOnly("138 0013-8000"))
    }

    @Test
    fun `normalize strips common mainland prefixes`() {
        assertEquals("13800138000", PhoneNumberMatcher.normalize("+86 138 0013 8000"))
        assertEquals("13800138000", PhoneNumberMatcher.normalize("008613800138000"))
        assertEquals("13800138000", PhoneNumberMatcher.normalize("8613800138000"))
    }

    @Test
    fun `normalize handles mainland landline with country code`() {
        assertEquals("1012345678", PhoneNumberMatcher.normalize("+861012345678"))
    }

    @Test
    fun `normalized mode matches landline with and without trunk prefix`() {
        assertTrue(
            PhoneNumberMatcher.matches(
                "01012345678",
                "+86 10 1234 5678",
                NumberMatchMode.NORMALIZED,
            ),
        )
    }

    @Test
    fun `normalized mode matches equivalent numbers`() {
        assertTrue(
            PhoneNumberMatcher.matches(
                "13800138000",
                "+86 138 0013 8000",
                NumberMatchMode.NORMALIZED,
            ),
        )
    }

    @Test
    fun `exact mode does not strip country code`() {
        assertFalse(
            PhoneNumberMatcher.matches(
                "13800138000",
                "+8613800138000",
                NumberMatchMode.EXACT,
            ),
        )
    }

    @Test
    fun `empty numbers never match`() {
        assertFalse(PhoneNumberMatcher.matches("", "", NumberMatchMode.NORMALIZED))
    }

    @Test
    fun `query variants include common stored forms`() {
        val variants = PhoneNumberMatcher.queryVariants("+8613800138000")

        assertTrue(variants.contains("13800138000"))
        assertTrue(variants.contains("8613800138000"))
        assertTrue(variants.contains("008613800138000"))
        assertTrue(variants.contains("+8613800138000"))
    }
}
