package com.unilens.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unilens.app.service.FloatingBubbleService
import com.unilens.app.update.UpdateInfo
import com.unilens.app.update.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("unilens_settings", Context.MODE_PRIVATE)
    private val updateManager = UpdateManager(application)

    private val _isPhoneEnabled = MutableStateFlow(prefs.getBoolean("phone_detection", true))
    val isPhoneEnabled: StateFlow<Boolean> = _isPhoneEnabled.asStateFlow()

    private val _isEmailEnabled = MutableStateFlow(prefs.getBoolean("email_detection", true))
    val isEmailEnabled: StateFlow<Boolean> = _isEmailEnabled.asStateFlow()

    private val _isFloatingEnabled = MutableStateFlow(prefs.getBoolean("floating_scanner", false))
    val isFloatingEnabled: StateFlow<Boolean> = _isFloatingEnabled.asStateFlow()

    private val _isAutoUpdateEnabled = MutableStateFlow(prefs.getBoolean("auto_update", true))
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_feedback", false))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    init {
        // Run background update check if enabled
        if (_isAutoUpdateEnabled.value) {
            checkForUpdates(force = false)
        }
    }

    fun setPhoneEnabled(enabled: Boolean) {
        _isPhoneEnabled.value = enabled
        prefs.edit().putBoolean("phone_detection", enabled).apply()
    }

    fun setEmailEnabled(enabled: Boolean) {
        _isEmailEnabled.value = enabled
        prefs.edit().putBoolean("email_detection", enabled).apply()
    }

    fun setFloatingEnabled(context: Context, enabled: Boolean): Boolean {
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return false // Caller needs to request overlay permission
            }
            startFloatingService(context)
        } else {
            stopFloatingService(context)
        }
        _isFloatingEnabled.value = enabled
        prefs.edit().putBoolean("floating_scanner", enabled).apply()
        return true
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        _isAutoUpdateEnabled.value = enabled
        prefs.edit().putBoolean("auto_update", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _isVibrationEnabled.value = enabled
        prefs.edit().putBoolean("vibration_feedback", enabled).apply()
    }

    fun checkForUpdates(force: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = updateManager.checkForUpdate(forceCheck = force)
            _isCheckingUpdate.value = false
            if (info != null) {
                _updateInfo.value = info
            } else if (force) {
                _statusNotice.value = "You are on the latest version."
            }
        }
    }

    fun startUpdateDownload(info: UpdateInfo) {
        viewModelScope.launch {
            _downloadProgress.value = 0
            val result = updateManager.downloadAndInstall(info) { progress ->
                _downloadProgress.value = progress
            }
            _downloadProgress.value = null
            result.onFailure { error ->
                _statusNotice.value = "Update failed: ${error.localizedMessage}"
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
        _downloadProgress.value = null
    }

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

    private fun startFloatingService(context: Context) {
        val intent = Intent(context, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopFloatingService(context: Context) {
        val intent = Intent(context, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_STOP
        }
        context.startService(intent)
    }
}
