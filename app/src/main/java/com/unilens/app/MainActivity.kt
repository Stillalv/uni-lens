package com.unilens.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.unilens.app.ui.components.UpdateDialog
import com.unilens.app.ui.screens.HomeScreen
import com.unilens.app.ui.screens.SettingsScreen
import com.unilens.app.ui.theme.UniLensTheme
import com.unilens.app.ui.viewmodel.SettingsViewModel

enum class MainDestination {
    HOME,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val scannerViewModel: com.unilens.app.ui.viewmodel.ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UniLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
                    val isFloatingEnabled by settingsViewModel.isFloatingEnabled.collectAsState()
                    val updateInfo by settingsViewModel.updateInfo.collectAsState()
                    val downloadProgress by settingsViewModel.downloadProgress.collectAsState()

                    // Check for in-app update dialog
                    if (updateInfo != null) {
                        UpdateDialog(
                            updateInfo = updateInfo!!,
                            downloadProgress = downloadProgress,
                            onUpdateClick = { settingsViewModel.startUpdateDownload(updateInfo!!) },
                            onDismiss = { settingsViewModel.dismissUpdateDialog() }
                        )
                    }

                    when (currentDestination) {
                        MainDestination.HOME -> {
                            HomeScreen(
                                scannerViewModel = scannerViewModel,
                                settingsViewModel = settingsViewModel,
                                isFloatingEnabled = isFloatingEnabled,
                                onStartScannerClick = {
                                    startActivity(Intent(this, ScannerActivity::class.java))
                                },
                                onOpenSettingsClick = {
                                    currentDestination = MainDestination.SETTINGS
                                }
                            )
                        }

                        MainDestination.SETTINGS -> {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                onBackClick = {
                                    currentDestination = MainDestination.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
