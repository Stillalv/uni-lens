package com.unilens.app.domain.normalizer

/**
 * Normalizes candidate texts extracted from OCR into canonical formats.
 * Guarded against over-correction: avoids global character replacements.
 */
object TextNormalizer {

    /**
     * Normalizes a phone number candidate:
     * - Preserves leading '+'
     * - Strips spaces, dashes, dots, parentheses, and other common formatting separators
     * - Returns null if the text does not start with '+'
     */
    fun normalizePhone(raw: String): String? {
        val trimmed = raw.trim()
        val plusIndex = trimmed.indexOf('+')
        if (plusIndex == -1) return null

        val candidate = trimmed.substring(plusIndex)
        // Keep '+' and only ASCII digits
        val sb = java.lang.StringBuilder("+")
        for (i in 1 until candidate.length) {
            val c = candidate[i]
            if (c.isDigit()) {
                sb.append(c)
            } else if (c == ' ' || c == '-' || c == '.' || c == '(' || c == ')' || c == '/' || c == '\u00A0') {
                // Allowed separators, skip
                continue
            } else {
                // Unexpected character encountered (e.g. letter or symbol) -> break candidate
                break
            }
        }

        val normalized = sb.toString()
        // Normalized phone must have '+' and at least 7 digits (min length 8: '+' plus 7 digits)
        return if (normalized.length >= 8) normalized else null
    }

    /**
     * Normalizes an email candidate:
     * - Trims edge whitespaces and trailing sentence punctuations (e.g. '.', ',', ';', ':', '!', ')')
     * - Safely collapses internal whitespace surrounding '@' if present (e.g. 'admin @ domain.com')
     */
    fun normalizeEmail(raw: String): String {
        var trimmed = raw.trim()

        // Strip trailing sentence punctuations
        while (trimmed.isNotEmpty() && (
                trimmed.endsWith(".") || trimmed.endsWith(",") ||
                trimmed.endsWith(";") || trimmed.endsWith(":") ||
                trimmed.endsWith("!") || trimmed.endsWith("?") ||
                trimmed.endsWith(")") || trimmed.endsWith(">")
            )) {
            trimmed = trimmed.substring(0, trimmed.length - 1).trim()
        }

        // Strip leading brackets if any
        while (trimmed.isNotEmpty() && (trimmed.startsWith("<") || trimmed.startsWith("("))) {
            trimmed = trimmed.substring(1).trim()
        }

        // Collapse whitespace around '@' if any
        if (trimmed.contains("@")) {
            val parts = trimmed.split("@")
            if (parts.size == 2) {
                trimmed = "${parts[0].trim()}@${parts[1].trim()}"
            }
        }

        return trimmed.lowercase()
    }
}
