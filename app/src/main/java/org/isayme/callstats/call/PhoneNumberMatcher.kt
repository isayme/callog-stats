package org.isayme.callstats.call

enum class NumberMatchMode {
    NORMALIZED,
    EXACT,
}

object PhoneNumberMatcher {

    fun digitsOnly(value: String): String = value.filter(Char::isDigit)

    /**
     * Removes separators and common mainland-China country-code prefixes.
     * Only full-number comparison is supported; no suffix/fuzzy matching.
     */
    fun normalize(value: String): String {
        var digits = digitsOnly(value)
        when {
            digits.length > 4 && digits.startsWith("0086") -> digits = digits.substring(4)
            digits.length >= 12 && digits.startsWith("86") -> digits = digits.substring(2)
        }
        return digits
    }

    fun key(value: String, mode: NumberMatchMode): String = when (mode) {
        NumberMatchMode.EXACT -> digitsOnly(value)
        NumberMatchMode.NORMALIZED -> normalize(value)
    }

    fun matches(first: String, second: String, mode: NumberMatchMode): Boolean {
        val firstKey = key(first, mode)
        val secondKey = key(second, mode)
        if (firstKey.isEmpty() || secondKey.isEmpty()) return false
        if (mode == NumberMatchMode.EXACT) return firstKey == secondKey

        // Mainland landlines are written both with and without the trunk prefix "0"
        // (for example 01012345678 and +86 10 1234 5678).
        return firstKey == secondKey || firstKey.removePrefix("0") == secondKey.removePrefix("0")
    }

    /**
     * CallLog can store the same number with different prefixes. Query a small set of
     * normalized variants, then filter precisely with [matches] in memory.
     */
    fun queryVariants(number: String): List<String> {
        val raw = number.trim()
        if (raw.isEmpty()) return emptyList()

        val digits = digitsOnly(raw)
        val normalized = normalize(raw)
        val national = normalized.removePrefix("0")
        return buildSet {
            add(raw)
            add(digits)
            add(normalized)
            if (national.length in 10..11) {
                add(national)
                add("0$national")
                add("86$national")
                add("+86$national")
                add("0086$national")
            }
        }.filter { it.isNotEmpty() }
    }
}
