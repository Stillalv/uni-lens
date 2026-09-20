package com.unilens.app.data.model

import android.graphics.Rect

enum class EntityType {
    PHONE,
    EMAIL
}

data class DetectedEntity(
    val type: EntityType,
    val value: String, // Normalized value presented to the user
    val rawText: String, // Raw extracted candidate from OCR
    val boundingBox: Rect? = null,
    val detectedAt: Long = System.currentTimeMillis()
) {
    val uniqueKey: String
        get() = "${type.name}:$value"
}
