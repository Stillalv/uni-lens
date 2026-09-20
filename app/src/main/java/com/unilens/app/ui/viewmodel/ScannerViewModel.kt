package com.unilens.app.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.CameraControl
import androidx.lifecycle.ViewModel
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.domain.engine.DetectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FrameMetrics(
    val width: Int = 0,
    val height: Int = 0,
    val rotationDegrees: Int = 0
)

class ScannerViewModel(
    val detectionEngine: DetectionEngine = DetectionEngine()
) : ViewModel() {

    private val _detectedEntities = MutableStateFlow<List<DetectedEntity>>(emptyList())
    val detectedEntities: StateFlow<List<DetectedEntity>> = _detectedEntities.asStateFlow()

    private val _frameMetrics = MutableStateFlow(FrameMetrics())
    val frameMetrics: StateFlow<FrameMetrics> = _frameMetrics.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val knownConfirmedKeys = mutableSetOf<String>()

    fun onFrameAnalysisResult(
        entities: List<DetectedEntity>,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        context: Context,
        vibrationEnabled: Boolean
    ) {
        _frameMetrics.value = FrameMetrics(imageWidth, imageHeight, rotationDegrees)
        _detectedEntities.value = entities

        // Haptic feedback when a brand new confirmed entity is discovered
        if (vibrationEnabled) {
            for (entity in entities) {
                if (knownConfirmedKeys.add(entity.uniqueKey)) {
                    triggerShortVibration(context)
                    break
                }
            }
        }
    }

    fun toggleTorch(cameraControl: CameraControl?) {
        val newState = !_isTorchOn.value
        _isTorchOn.value = newState
        cameraControl?.enableTorch(newState)
    }

    fun copyToClipboard(context: Context, entity: DetectedEntity) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UniLens Detected", entity.value)
        clipboard.setPrimaryClip(clip)
        _toastMessage.value = "Copied to clipboard: ${entity.value}"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun triggerShortVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        detectionEngine.reset()
    }
}
