package com.unilens.app.domain.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EmailDetectorTest {

    private lateinit var detector: EmailDetector

    @Before
    fun setUp() {
        detector = EmailDetector()
    }

    @Test
    fun testValidEmailAddresses() {
        val validEmails = listOf(
            "admin@gmail.com",
            "john.doe@example.com",
            "user@domain.co.id",
            "test+foo@gmail.com",
            "support-desk@service.org"
        )

        for (email in validEmails) {
            val result = detector.detect("Reach us at $email for support")
            assertEquals("Expected 1 detection for $email", 1, result.size)
            assertEquals(email.lowercase(), result[0].value)
        }
    }

    @Test
    fun testInvalidEmailAddressesAreRejected() {
        val invalidEmails = listOf(
            "admin@",
            "@gmail.com",
            "admin@gmail",
            "admin",
            "plainaddress",
            "#@%^%#$@#$@#.com",
            "@example.com"
        )

        for (item in invalidEmails) {
            val result = detector.detect("Invalid email token: $item here")
            assertTrue("Expected 0 detection for invalid email: $item", result.isEmpty())
        }
    }

    @Test
    fun testEmailWithSurroundingPunctuation() {
        val result = detector.detect("Email me: (john@example.com). Thanks!")
        assertEquals(1, result.size)
        assertEquals("john@example.com", result[0].value)
    }
}
