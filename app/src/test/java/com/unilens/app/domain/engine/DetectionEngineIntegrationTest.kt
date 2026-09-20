package com.unilens.app.domain.engine

import com.unilens.app.data.model.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetectionEngineIntegrationTest {

    private lateinit var engine: DetectionEngine

    @Before
    fun setUp() {
        engine = DetectionEngine()
    }

    /**
     * PRD Section 49 Integration Test:
     * Input:
     * "Google Gmail Youtube +573178202866 Status 24:28 Price 0.028$ admin@example.com"
     * Expected:
     * PHONE: +573178202866
     * EMAIL: admin@example.com
     * Does not detect 24:28 or 0.028$
     */
    @Test
    fun testPrdSection49Integration() {
        val sampleText = "Google Gmail Youtube +573178202866 Status 24:28 Price 0.028$ admin@example.com"

        val detectedEntities = engine.detectFromText(sampleText)

        assertEquals("Expected exactly 2 detected entities", 2, detectedEntities.size)

        val phoneEntity = detectedEntities.find { it.type == EntityType.PHONE }
        val emailEntity = detectedEntities.find { it.type == EntityType.EMAIL }

        assertTrue("Phone entity must be present", phoneEntity != null)
        assertEquals("+573178202866", phoneEntity?.value)

        assertTrue("Email entity must be present", emailEntity != null)
        assertEquals("admin@example.com", emailEntity?.value)

        // Ensure 24:28 and 0.028 are not present
        val allValues = detectedEntities.map { it.value }
        assertFalse("Must not detect 24:28", allValues.any { it.contains("24:28") || it.contains("24.28") })
        assertFalse("Must not detect 0.028", allValues.any { it.contains("0.028") })
    }

    @Test
    fun testDeduplicationAndToggle() {
        val textWithDuplicates = "+628123456789 and again +62 812 3456 789 with email test@example.com and test@example.com"

        val detected = engine.detectFromText(textWithDuplicates)

        // Phone +628123456789 appears in two formats, should be deduplicated to 1 phone entity
        val phoneDetections = detected.filter { it.type == EntityType.PHONE }
        val emailDetections = detected.filter { it.type == EntityType.EMAIL }

        assertEquals(1, phoneDetections.size)
        assertEquals("+628123456789", phoneDetections[0].value)

        assertEquals(1, emailDetections.size)
        assertEquals("test@example.com", emailDetections[0].value)

        // Test disabling phone
        engine.isPhoneDetectionEnabled = false
        val emailOnly = engine.detectFromText(textWithDuplicates)
        assertEquals(1, emailOnly.size)
        assertEquals(EntityType.EMAIL, emailOnly[0].type)
    }

    @Test
    fun testStaticImageTextDetection() {
        val attachedImageText = "Contact card:\nName: John Doe\nPhone: +62 812-3456-7890\nOffice: 0215551234\nEmail: john.doe@company.com"
        val detected = engine.detectFromText(attachedImageText)

        assertEquals(2, detected.size)
        assertEquals("+6281234567890", detected.find { it.type == EntityType.PHONE }?.value)
        assertEquals("john.doe@company.com", detected.find { it.type == EntityType.EMAIL }?.value)
    }
}
