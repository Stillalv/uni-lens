package com.unilens.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unilens.app.ui.components.UpdateDialog
import com.unilens.app.ui.theme.EmeraldPrimary
import com.unilens.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isPhoneEnabled by settingsViewModel.isPhoneEnabled.collectAsState()
    val isEmailEnabled by settingsViewModel.isEmailEnabled.collectAsState()
    val isFloatingEnabled by settingsViewModel.isFloatingEnabled.collectAsState()
    val isAutoUpdateEnabled by settingsViewModel.isAutoUpdateEnabled.collectAsState()
    val isVibrationEnabled by settingsViewModel.isVibrationEnabled.collectAsState()

    val isCheckingUpdate by settingsViewModel.isCheckingUpdate.collectAsState()
    val updateInfo by settingsViewModel.updateInfo.collectAsState()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsState()
    val statusNotice by settingsViewModel.statusNotice.collectAsState()

    LaunchedEffect(statusNotice) {
        statusNotice?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearStatusNotice()
        }
    }

    if (updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            downloadProgress = downloadProgress,
            onUpdateClick = { settingsViewModel.startUpdateDownload(updateInfo!!) },
            onDismiss = { settingsViewModel.dismissUpdateDialog() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Detectors
            Text(
                text = "DETECTION ENGINE",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Phone Detection",
                        subtitle = "Detect numbers starting with '+' (E.164)",
                        checked = isPhoneEnabled,
                        onCheckedChange = { settingsViewModel.setPhoneEnabled(it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    SettingToggleRow(
                        title = "Email Detection",
                        subtitle = "Detect name@domain.extension format",
                        checked = isEmailEnabled,
                        onCheckedChange = { settingsViewModel.setEmailEnabled(it) }
                    )
                }
            }

            // Section 2: Features
            Text(
                text = "INTERACTION & OVERLAY",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Floating Scanner",
                        subtitle = "Show movable bubble over other apps",
                        checked = isFloatingEnabled,
                        onCheckedChange = { enable ->
                            val success = settingsViewModel.setFloatingEnabled(context, enable)
                            if (!success && enable) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Overlay permission required.")
                                }
                                openOverlaySettings(context)
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    SettingToggleRow(
                        title = "Vibration Feedback",
                        subtitle = "Short haptic pulse when new entity is found",
                        checked = isVibrationEnabled,
                        onCheckedChange = { settingsViewModel.setVibrationEnabled(it) }
                    )
                }
            }

            // Section 3: Updates
            Text(
                text = "UPDATES & RELEASES",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Automatic Check",
                        subtitle = "Check GitHub Releases on launch",
                        checked = isAutoUpdateEnabled,
                        onCheckedChange = { settingsViewModel.setAutoUpdateEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { settingsViewModel.checkForUpdates(force = true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Updates Now")
                        }
                    }
                }
            }

            // Section 4: Privacy & About
            Text(
                text = "PRIVACY & ABOUT",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Guarantee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "UniLens processes all camera frames 100% locally on your device using on-device ML Kit OCR. Camera images, video frames, and scanned text are never sent to any server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "UniLens v1.0.0 (API 23+)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = EmeraldPrimary
            )
        )
    }
}

private fun openOverlaySettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
