package com.unilens.app.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.domain.engine.DetectionEngine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX ImageAnalysis analyzer with STRATEGY_KEEP_ONLY_LATEST and throttling.
 * Keeps CPU and battery cool by executing OCR at ~7-10 FPS without queue backlog.
 */
class CameraAnalyzer(
    private val detectionEngine: DetectionEngine,
    private val onDetectionsUpdated: (entities: List<DetectedEntity>, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit
) : ImageAnalysis.Analyzer {

    // Bundled on-device Latin text recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val isBusy = AtomicBoolean(false)
    private var lastAnalysisTimestamp = 0L
    private val minAnalysisIntervalMs = 120L // ~8 FPS max

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // 1. Throttle frame rate & drop frame if previous analysis is still ongoing
        if (currentTime - lastAnalysisTimestamp < minAnalysisIntervalMs || isBusy.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        if (!isBusy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        lastAnalysisTimestamp = currentTime
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val detected = detectionEngine.processMlKitText(visionText)
                onDetectionsUpdated(
                    detected,
                    imageProxy.width,
                    imageProxy.height,
                    rotationDegrees
                )
            }
            .addOnFailureListener {
                // Ignore transient OCR failures gracefully without freezing
            }
            .addOnCompleteListener {
                try {
                    imageProxy.close()
                } catch (_: Exception) {
                } finally {
                    isBusy.set(false)
                }
            }
    }

    fun close() {
        try {
            textRecognizer.close()
        } catch (_: Exception) {
        }
    }
}
