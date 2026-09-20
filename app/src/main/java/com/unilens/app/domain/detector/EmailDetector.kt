package com.unilens.app.domain.detector

import android.graphics.Rect
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.data.model.EntityType
import com.unilens.app.domain.normalizer.TextNormalizer
import java.util.regex.Pattern

/**
 * Detector for email addresses.
 * Pattern: LOCAL_PART + @ + DOMAIN + . + EXTENSION
 *
 * Valid examples:
 * - admin@gmail.com
 * - john@example.com
 * - john.doe@example.com
 * - user123@domain.co.id
 * - test+foo@gmail.com
 *
 * Invalid examples:
 * - admin@
 * - @gmail.com
 * - admin@gmail
 * - admin
 */
class EmailDetector : TextDetector {

    companion object {
        // Broad search regex to capture potential email candidates (including possible accidental space around @)
        private val EMAIL_CANDIDATE_REGEX = Pattern.compile(
            """[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+\s*@\s*[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)+"""
        )

        // Strict email validation regex
        private val STRICT_EMAIL_REGEX = Pattern.compile(
            """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,}$"""
        )
    }

    override fun detect(text: String, boundingBox: Rect?): List<DetectedEntity> {
        val results = mutableListOf<DetectedEntity>()
        val matcher = EMAIL_CANDIDATE_REGEX.matcher(text)

        while (matcher.find()) {
            val candidate = matcher.group()
            val normalized = TextNormalizer.normalizeEmail(candidate)

            if (isValidEmail(normalized)) {
                results.add(
                    DetectedEntity(
                        type = EntityType.EMAIL,
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
     * Validates an email candidate against standard requirements:
     * - Has username before '@'
     * - Has domain after '@'
     * - Has '.' followed by at least 2 alpha characters for top-level domain
     */
    fun isValidEmail(email: String): Boolean {
        if (!email.contains("@")) return false
        val parts = email.split("@")
        if (parts.size != 2) return false

        val local = parts[0]
        val domain = parts[1]

        if (local.isEmpty() || domain.isEmpty()) return false
        if (!domain.contains(".")) return false

        val extension = domain.substringAfterLast(".", "")
        if (extension.length < 2 || !extension.all { it.isLetter() }) return false

        return STRICT_EMAIL_REGEX.matcher(email).matches()
    }
}
