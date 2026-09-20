package com.unilens.app.domain.detector

import android.graphics.Rect
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.data.model.EntityType
import com.unilens.app.domain.normalizer.TextNormalizer
import java.util.regex.Pattern

/**
 * Detector for international phone numbers.
 * STRICT REQUIREMENTS:
 * 1. Must explicitly start with '+'
 * 2. Country code must begin with [1-9]
 * 3. Total digits must be between 7 and 15 (E.164 standard)
 * 4. Numbers without '+' (e.g. 08123456789, 628123456789) MUST BE REJECTED.
 * 5. General numbers, decimals, timestamps, prices (e.g. 24.28, 11.1269, 0.028, 2026, 24:28) MUST BE REJECTED.
 */
class PhoneDetector : TextDetector {

    companion object {
        // Candidate search: starts with '+' followed by non-zero digit and formatting characters
        private val PHONE_CANDIDATE_REGEX = Pattern.compile(
            """(?<!\w)\+([1-9][0-9\s\-./()]{6,24})(?!\w)"""
        )

        // Strict validation regex on the normalized result
        private val NORMALIZED_PHONE_REGEX = Pattern.compile(
            """^\+[1-9][0-9]{6,14}$"""
        )
    }

    override fun detect(text: String, boundingBox: Rect?): List<DetectedEntity> {
        val results = mutableListOf<DetectedEntity>()
        val matcher = PHONE_CANDIDATE_REGEX.matcher(text)

        while (matcher.find()) {
            val candidate = matcher.group().trim()

            // Skip if candidate looks like a timestamp, ratio, or price (e.g. +12:34 or contains $)
            if (candidate.contains(":") || candidate.contains("$") || candidate.contains("€")) {
                continue
            }

            val normalized = TextNormalizer.normalizePhone(candidate) ?: continue

            // Must strictly match normalized E.164 pattern
            if (isValidNormalizedPhone(normalized)) {
                results.add(
                    DetectedEntity(
                        type = EntityType.PHONE,
                        value = normalized,
                        rawText = candidate,
                        boundingBox = boundingBox
                    )
                )
            }
        }

        return results
    }

    /**
     * Validates if a normalized string is a valid international phone number:
     * - Starts with '+'
     * - Country code digit is 1-9
     * - 7 to 15 digits total
     */
    fun isValidNormalizedPhone(normalized: String): Boolean {
        return NORMALIZED_PHONE_REGEX.matcher(normalized).matches()
    }
}
