package com.unilens.app.domain.normalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun testNormalizePhone() {
        assertEquals("+6281234567890", TextNormalizer.normalizePhone("+62 812-3456-7890"))
        assertEquals("+6281234567890", TextNormalizer.normalizePhone("+62.812.3456.7890"))
        assertEquals("+6281234567890", TextNormalizer.normalizePhone("+62 (812) 3456 7890"))
        assertEquals("+14155552671", TextNormalizer.normalizePhone("  +1-415-555-2671  "))

        // Rejection without '+'
        assertNull(TextNormalizer.normalizePhone("081234567890"))
        assertNull(TextNormalizer.normalizePhone("6281234567890"))
    }

    @Test
    fun testNormalizeEmail() {
        assertEquals("admin@gmail.com", TextNormalizer.normalizeEmail("admin @ gmail.com"))
        assertEquals("user@example.com", TextNormalizer.normalizeEmail("<user@example.com>"))
        assertEquals("info@company.co.id", TextNormalizer.normalizeEmail("info@company.co.id."))
        assertEquals("contact@domain.com", TextNormalizer.normalizeEmail("(contact@domain.com)"))
    }
}
