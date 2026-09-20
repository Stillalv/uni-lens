package com.unilens.app.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Phone
import com.unilens.app.ui.components.ImageScanResultsDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.unilens.app.camera.CameraAnalyzer
import com.unilens.app.data.model.DetectedEntity
import com.unilens.app.data.model.EntityType
import com.unilens.app.ui.components.BoundingBoxOverlay
import com.unilens.app.ui.theme.DarkSurface
import com.unilens.app.ui.theme.EmailColor
import com.unilens.app.ui.theme.EmeraldPrimary
import com.unilens.app.ui.theme.PhoneColor
import com.unilens.app.ui.viewmodel.ScannerViewModel
import com.unilens.app.ui.viewmodel.SettingsViewModel
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    scannerViewModel: ScannerViewModel,
    settingsViewModel: SettingsViewModel,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val detectedEntities by scannerViewModel.detectedEntities.collectAsState()
    val frameMetrics by scannerViewModel.frameMetrics.collectAsState()
    val isTorchOn by scannerViewModel.isTorchOn.collectAsState()
    val toastMessage by scannerViewModel.toastMessage.collectAsState()

    val selectedImageUri by scannerViewModel.selectedImageUri.collectAsState()
    val isAnalyzingImage by scannerViewModel.isAnalyzingImage.collectAsState()
    val imageScanResults by scannerViewModel.imageScanResults.collectAsState()

    val isPhoneEnabled by settingsViewModel.isPhoneEnabled.collectAsState()
    val isEmailEnabled by settingsViewModel.isEmailEnabled.collectAsState()
    val isVibrationEnabled by settingsViewModel.isVibrationEnabled.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scannerViewModel.analyzeImageUri(context, it, isVibrationEnabled)
        }
    }

    var cameraControlRef by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Sync detection flags to engine
    LaunchedEffect(isPhoneEnabled, isEmailEnabled) {
        scannerViewModel.detectionEngine.isPhoneDetectionEnabled = isPhoneEnabled
        scannerViewModel.detectionEngine.isEmailDetectionEnabled = isEmailEnabled
    }

    // Handle copy toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            scannerViewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fullscreen Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analyzer = CameraAnalyzer(scannerViewModel.detectionEngine) { entities, width, height, rotation ->
                        scannerViewModel.onFrameAnalysisResult(
                            entities = entities,
                            imageWidth = width,
                            imageHeight = height,
                            rotationDegrees = rotation,
                            context = ctx,
                            vibrationEnabled = isVibrationEnabled
                        )
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, analyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControlRef = camera
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 2. Real-time Bounding Box Highlights
        BoundingBoxOverlay(
            entities = detectedEntities,
            frameMetrics = frameMetrics,
            modifier = Modifier.fillMaxSize()
        )

        // Image Scan Results Dialog (if image attached)
        if (selectedImageUri != null) {
            ImageScanResultsDialog(
                imageUri = selectedImageUri!!,
                isAnalyzing = isAnalyzingImage,
                results = imageScanResults,
                onCopyClick = { entity -> scannerViewModel.copyToClipboard(context, entity) },
                onPickAnother = { imagePickerLauncher.launch("image/*") },
                onDismiss = { scannerViewModel.clearSelectedImage() }
            )
        }

        // 3. Top Action Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Detection Count Chip
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Detected: ${detectedEntities.size}",
                    color = if (detectedEntities.isNotEmpty()) EmeraldPrimary else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Right Action Controls: Attach Image & Torch
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Attach Image",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { scannerViewModel.toggleTorch(cameraControlRef?.cameraControl) },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) EmeraldPrimary else Color.White
                    )
                }
            }
        }

        // 4. Bottom Detected Results Tray
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            if (detectedEntities.isEmpty()) {
                // Instruction banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Point camera at text with phone (+ required) or email",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detectedEntities, key = { it.uniqueKey }) { entity ->
                        DetectedEntityCard(
                            entity = entity,
                            onCopyClick = { scannerViewModel.copyToClipboard(context, entity) }
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
fun DetectedEntityCard(
    entity: DetectedEntity,
    onCopyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = if (entity.type == EntityType.PHONE) Icons.Default.Phone else Icons.Default.Email
                val accent = if (entity.type == EntityType.PHONE) PhoneColor else EmailColor

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entity.value,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (entity.type == EntityType.PHONE) "PHONE" else "EMAIL",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCopyClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "COPY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
