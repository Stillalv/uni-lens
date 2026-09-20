package com.unilens.app.domain.detector

import android.graphics.Rect
import com.unilens.app.data.model.DetectedEntity

/**
 * Common detector interface for processing text snippets.
 */
interface TextDetector {
    fun detect(text: String, boundingBox: Rect? = null): List<DetectedEntity>
}
