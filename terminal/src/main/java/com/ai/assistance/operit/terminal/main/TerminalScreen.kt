package com.ai.assistance.operit.terminal.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ai.assistance.operit.terminal.TerminalEnv
import com.ai.assistance.operit.terminal.TerminalManager
import com.ai.assistance.operit.terminal.ui.CoryScaffold

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerminalScreen(
    env: TerminalEnv
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }
    val manifestSoftInputMode = remember(hostActivity) { hostActivity?.manifestSoftInputMode() }
    val manager = remember { TerminalManager.getInstance(context) }

    // Prevent soft keyboard from resizing the terminal view.
    DisposableEffect(hostActivity, manifestSoftInputMode) {
        hostActivity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            val window = hostActivity?.window
            if (window != null && manifestSoftInputMode != null) {
                window.setSoftInputMode(manifestSoftInputMode)
            }
        }
    }

    // Auto-create the first session if none exist.
    LaunchedEffect(manager) {
        if (manager.sessions.value.isEmpty()) {
            manager.createNewSession()
        }
    }

    CoryScaffold(env = env)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Activity.manifestSoftInputMode(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getActivityInfo(
            componentName,
            PackageManager.ComponentInfoFlags.of(0)
        ).softInputMode
    } else {
        @Suppress("DEPRECATION")
        packageManager.getActivityInfo(componentName, 0).softInputMode
    }
