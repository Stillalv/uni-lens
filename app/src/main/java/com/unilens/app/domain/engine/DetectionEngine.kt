package com.unilens.app.domain.engine

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.domain.detector.EmailDetector
import com.unilens.app.domain.detector.PhoneDetector
import com.unilens.app.domain.stabilizer.TemporalStabilizer

/**
 * High-level detection engine that coordinates phone and email detectors,
 * extracts candidates from ML Kit OCR blocks/lines, and stabilizes detections.
 */
class DetectionEngine(
    val phoneDetector: PhoneDetector = PhoneDetector(),
    val emailDetector: EmailDetector = EmailDetector(),
    val stabilizer: TemporalStabilizer = TemporalStabilizer()
) {
    @Volatile
    var isPhoneDetectionEnabled: Boolean = true

    @Volatile
    var isEmailDetectionEnabled: Boolean = true

    /**
     * Detects entities from an arbitrary string (used in unit tests and text feeds).
     */
    fun detectFromText(text: String, boundingBox: Rect? = null): List<DetectedEntity> {
        val candidates = mutableListOf<DetectedEntity>()

        if (isPhoneDetectionEnabled) {
            candidates.addAll(phoneDetector.detect(text, boundingBox))
        }
        if (isEmailDetectionEnabled) {
            candidates.addAll(emailDetector.detect(text, boundingBox))
        }

        // Return deduplicated list by unique key
        return candidates.distinctBy { it.uniqueKey }
    }

    /**
     * Processes ML Kit Text hierarchy: checks lines and elements to associate precise bounding boxes.
     */
    fun processMlKitText(
        mlKitText: Text,
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<DetectedEntity> {
        val rawFrameDetections = mutableListOf<DetectedEntity>()

        for (block in mlKitText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                val lineBox = line.boundingBox

                // Search within line
                if (isPhoneDetectionEnabled) {
                    rawFrameDetections.addAll(phoneDetector.detect(lineText, lineBox))
                }
                if (isEmailDetectionEnabled) {
                    rawFrameDetections.addAll(emailDetector.detect(lineText, lineBox))
                }
            }
        }

        // If line-level search missed something spanning multiple elements or in block text,
        // perform fallback check on entire text block
        if (rawFrameDetections.isEmpty()) {
            val fullText = mlKitText.text
            if (fullText.isNotBlank()) {
                rawFrameDetections.addAll(detectFromText(fullText, null))
            }
        }

        // Pass frame detections through stabilizer to eliminate visual flicker
        return stabilizer.update(rawFrameDetections.distinctBy { it.uniqueKey }, currentTimeMs)
    }

    fun reset() {
        stabilizer.clear()
    }
}
