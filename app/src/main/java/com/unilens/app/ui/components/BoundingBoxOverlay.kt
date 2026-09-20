package com.unilens.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.unilens.app.camera.CoordinateTransformer
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.data.model.EntityType
import com.unilens.app.ui.theme.EmailColor
import com.unilens.app.ui.theme.PhoneColor
import com.unilens.app.ui.viewmodel.FrameMetrics

/**
 * Canvas overlay that draws highlighted bounding boxes around detected phone & email texts.
 */
@Composable
fun BoundingBoxOverlay(
    entities: List<DetectedEntity>,
    frameMetrics: FrameMetrics,
    modifier: Modifier = Modifier
) {
    if (entities.isEmpty() || frameMetrics.width <= 0 || frameMetrics.height <= 0) {
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val viewWidth = size.width
        val viewHeight = size.height

        for (entity in entities) {
            val rect = entity.boundingBox ?: continue
            val transformed = CoordinateTransformer.transform(
                rect = rect,
                imageWidth = frameMetrics.width,
                imageHeight = frameMetrics.height,
                rotationDegrees = frameMetrics.rotationDegrees,
                viewWidth = viewWidth,
                viewHeight = viewHeight
            )

            val boxColor = if (entity.type == EntityType.PHONE) PhoneColor else EmailColor
            val strokeWidthPx = 3.5f * density

            // Draw bounding rounded box
            drawRoundRect(
                color = boxColor,
                topLeft = Offset(transformed.left, transformed.top),
                size = Size(transformed.width(), transformed.height()),
                cornerRadius = CornerRadius(8f * density, 8f * density),
                style = Stroke(width = strokeWidthPx)
            )

            // Draw subtle corner accents / indicator dot
            drawCircle(
                color = boxColor,
                radius = 4f * density,
                center = Offset(transformed.left, transformed.top)
            )
        }
    }
}
