package com.unilens.app.domain.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhoneDetectorTest {

    private lateinit var detector: PhoneDetector

    @Before
    fun setUp() {
        detector = PhoneDetector()
    }

    @Test
    fun testValidPhoneNumbersStartingWithPlus() {
        val validNumbers = listOf(
            "+628123456789",
            "+573178202866",
            "+14155552671",
            "+442071838750"
        )

        for (num in validNumbers) {
            val result = detector.detect("Call us at $num now")
            assertEquals("Expected 1 detection for $num", 1, result.size)
            assertEquals(num, result[0].value)
        }
    }

    @Test
    fun testPhoneSeparatorsNormalization() {
        val cases = mapOf(
            "+62 812 3456 7890" to "+6281234567890",
            "+62-812-3456-7890" to "+6281234567890",
            "+62 812-3456-7890" to "+6281234567890",
            "+1 (415) 555-2671" to "+14155552671"
        )

        for ((input, expected) in cases) {
            val result = detector.detect("Contact: $input")
            assertEquals("Expected 1 detection for $input", 1, result.size)
            assertEquals("Normalized value mismatch for $input", expected, result[0].value)
        }
    }

    @Test
    fun testNumbersWithoutPlusAreStrictlyRejected() {
        val invalidNumbers = listOf(
            "08123456789",
            "628123456789",
            "8123456789",
            "123456789",
            "081234567890"
        )

        for (num in invalidNumbers) {
            val result = detector.detect("Check this number: $num")
            assertTrue("Expected 0 detection for number without '+': $num", result.isEmpty())
        }
    }

    @Test
    fun testFalsePositiveRejection() {
        val falsePositives = listOf(
            "24.28",
            "11.1269",
            "0.028",
            "2026",
            "123456",
            "24:28",
            "0.028$"
        )

        for (item in falsePositives) {
            val result = detector.detect("Text contains $item in report")
            assertTrue("Expected 0 detection for false positive: $item", result.isEmpty())
        }
    }
}
