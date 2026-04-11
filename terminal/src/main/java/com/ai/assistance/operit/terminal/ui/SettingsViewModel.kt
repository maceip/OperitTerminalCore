package com.ai.assistance.operit.terminal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.assistance.operit.terminal.TerminalManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    application: Application,
    private val terminalManager: TerminalManager? = null
) : AndroidViewModel(application) {

    private val _cacheSize = MutableStateFlow("Click to refresh")
    val cacheSize = _cacheSize.asStateFlow()

    private val _isCalculatingCache = MutableStateFlow(false)
    val isCalculatingCache = _isCalculatingCache.asStateFlow()

    // ---- Cache ----

    fun getCacheSize() {
        viewModelScope.launch {
            _isCalculatingCache.value = true
            try {
                val filesDir = getApplication<Application>().filesDir
                val size = calculateDirSize(filesDir)
                _cacheSize.value = formatSize(size)
            } catch (_: Exception) {
                _cacheSize.value = "Error calculating size"
            } finally {
                _isCalculatingCache.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                val filesDir = getApplication<Application>().filesDir
                listOf("usr", "home", "tmp").forEach { dir ->
                    File(filesDir, dir).deleteRecursively()
                }
                _cacheSize.value = "Environment reset. Restart app."
            } catch (e: Exception) {
                _cacheSize.value = "Reset failed: ${e.message}"
            }
        }
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
