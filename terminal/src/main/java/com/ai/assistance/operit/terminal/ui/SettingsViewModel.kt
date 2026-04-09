package com.ai.assistance.operit.terminal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.assistance.operit.terminal.utils.UpdateChecker
import com.ai.assistance.operit.terminal.TerminalManager
import com.ai.assistance.operit.terminal.data.SSHConfig
import com.ai.assistance.operit.terminal.utils.SSHConfigManager
import com.ai.assistance.operit.terminal.utils.VirtualKeyboardConfigManager
import com.ai.assistance.operit.terminal.utils.VirtualKeyboardLayoutConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    application: Application,
    private val terminalManager: TerminalManager? = null
) : AndroidViewModel(application) {

    private val terminalManagerRef by lazy { terminalManager ?: TerminalManager.getInstance(application) }
    private val updateChecker = UpdateChecker(application)
    private val sshConfigManager = SSHConfigManager(application)
    private val virtualKeyboardConfigManager = VirtualKeyboardConfigManager.getInstance(application)

    private val prefs = application.getSharedPreferences("terminal_settings", android.content.Context.MODE_PRIVATE)

    private val _cacheSize = MutableStateFlow(application.getString(com.ai.assistance.operit.terminal.R.string.cache_size_default))
    val cacheSize = _cacheSize.asStateFlow()

    private val _updateStatus = MutableStateFlow(application.getString(com.ai.assistance.operit.terminal.R.string.update_status_default))
    val updateStatus = _updateStatus.asStateFlow()

    private val _isCalculatingCache = MutableStateFlow(false)
    val isCalculatingCache = _isCalculatingCache.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache = _isClearingCache.asStateFlow()

    private val _hasUpdateAvailable = MutableStateFlow(false)
    val hasUpdateAvailable = _hasUpdateAvailable.asStateFlow()

    // SSH config state
    private val _sshConfig = MutableStateFlow<SSHConfig?>(null)
    val sshConfig = _sshConfig.asStateFlow()

    private val _sshEnabled = MutableStateFlow(false)
    val sshEnabled = _sshEnabled.asStateFlow()

    private val _virtualKeyboardLayout = MutableStateFlow(virtualKeyboardConfigManager.loadLayout())
    val virtualKeyboardLayout = _virtualKeyboardLayout.asStateFlow()

    init {
        checkForUpdates()
        loadSSHConfigs()
        loadSSHEnabled()
        loadVirtualKeyboardLayout()
    }

    // ---- Cache ----

    fun getCacheSize() {
        viewModelScope.launch {
            _isCalculatingCache.value = true
            try {
                val filesDir = getApplication<Application>().filesDir
                val size = calculateDirSize(filesDir)
                _cacheSize.value = formatSize(size)
            } catch (_: Exception) {
                _cacheSize.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.cache_calculation_failed)
            } finally {
                _isCalculatingCache.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            try {
                val filesDir = getApplication<Application>().filesDir
                // Delete usr, home, tmp — bootstrap will recreate them
                listOf("usr", "home", "tmp").forEach { dir ->
                    File(filesDir, dir).deleteRecursively()
                }
                _cacheSize.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.environment_reset_complete)
            } catch (e: Exception) {
                _cacheSize.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.environment_reset_failed, e.message ?: "")
            } finally {
                _isClearingCache.value = false
            }
        }
    }

    // ---- Updates ----

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateStatus.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.checking_updates)
            when (val result = updateChecker.checkForUpdates(showToast = true)) {
                is UpdateChecker.UpdateResult.UpdateAvailable -> {
                    _updateStatus.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.update_available, result.latestVersion, result.currentVersion)
                    _hasUpdateAvailable.value = true
                }
                is UpdateChecker.UpdateResult.UpToDate -> {
                    _updateStatus.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.up_to_date, result.currentVersion)
                    _hasUpdateAvailable.value = false
                }
                is UpdateChecker.UpdateResult.Error -> {
                    _updateStatus.value = getApplication<Application>().getString(com.ai.assistance.operit.terminal.R.string.update_check_failed, result.message)
                    _hasUpdateAvailable.value = false
                }
            }
        }
    }

    fun openGitHubRepo() = updateChecker.openGitHubRepo()
    fun openGitHubReleases() = updateChecker.openGitHubReleases()

    // ---- SSH ----

    private fun loadSSHConfigs() {
        viewModelScope.launch { _sshConfig.value = sshConfigManager.getConfig() }
    }

    private fun loadSSHEnabled() {
        _sshEnabled.value = sshConfigManager.isEnabled()
    }

    fun saveSSHConfig(config: SSHConfig) {
        viewModelScope.launch {
            sshConfigManager.saveConfig(config)
            loadSSHConfigs()
        }
    }

    fun deleteSSHConfig() {
        viewModelScope.launch {
            sshConfigManager.deleteConfig()
            sshConfigManager.setEnabled(false)
            loadSSHConfigs()
            loadSSHEnabled()
        }
    }

    fun setSSHEnabled(enabled: Boolean) {
        sshConfigManager.setEnabled(enabled)
        loadSSHEnabled()
    }

    // ---- Virtual keyboard ----

    private fun loadVirtualKeyboardLayout() {
        _virtualKeyboardLayout.value = virtualKeyboardConfigManager.loadLayout()
    }

    fun saveVirtualKeyboardLayout(layout: VirtualKeyboardLayoutConfig) {
        virtualKeyboardConfigManager.saveLayout(layout)
        loadVirtualKeyboardLayout()
    }

    // ---- Helpers ----

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "%.2f GB".format(gb)
            mb >= 1.0 -> "%.2f MB".format(mb)
            else -> "%.2f KB".format(kb)
        }
    }
}
