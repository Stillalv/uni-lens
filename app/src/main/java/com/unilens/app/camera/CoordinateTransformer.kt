package com.unilens.app.camera

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF

/**
 * Transforms ML Kit bounding box coordinates from camera sensor coordinate space
 * to UI view coordinates taking into account camera rotation and aspect-ratio scaling.
 */
object CoordinateTransformer {

    fun transform(
        rect: Rect,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        viewWidth: Float,
        viewHeight: Float,
        isFrontFacing: Boolean = false
    ): RectF {
        val matrix = Matrix()

        // 1. Handle mirror for front camera
        if (isFrontFacing) {
            matrix.postScale(-1f, 1f, imageWidth / 2f, imageHeight / 2f)
        }

        // 2. Handle rotation
        matrix.postRotate(rotationDegrees.toFloat(), imageWidth / 2f, imageHeight / 2f)

        // Effective buffer dimensions after rotation
        val rotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight else imageWidth
        val rotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth else imageHeight

        // 3. Center-crop scale to fit view
        val scaleX = viewWidth / rotatedWidth.toFloat()
        val scaleY = viewHeight / rotatedHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)

        val offsetX = (viewWidth - rotatedWidth * scale) / 2f
        val offsetY = (viewHeight - rotatedHeight * scale) / 2f

        matrix.postScale(scale, scale)
        matrix.postTranslate(offsetX, offsetY)

        val src = RectF(rect)
        val dst = RectF()
        matrix.mapRect(dst, src)
        return dst
    }
}
