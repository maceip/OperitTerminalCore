package com.ai.assistance.operit.terminal.main

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ai.assistance.operit.terminal.TerminalEnv
import com.ai.assistance.operit.terminal.TerminalManager
import com.ai.assistance.operit.terminal.ui.SetupScreen
import com.ai.assistance.operit.terminal.ui.SettingsScreen
import com.ai.assistance.operit.terminal.ui.CoryScaffold
import com.ai.assistance.operit.terminal.utils.UpdateChecker
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerminalScreen(
    env: TerminalEnv
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

    val manager = remember { TerminalManager.getInstance(context) }

    // 更新检查器
    val updateChecker = remember { UpdateChecker(context) }

    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        startDestination = when {
            env.forceShowSetup -> TerminalRoutes.SETUP_ROUTE
            isFirstLaunch -> TerminalRoutes.SETUP_ROUTE
            else -> TerminalRoutes.TERMINAL_HOME_ROUTE
        }

        // 后台静默检查更新，不显示 Toast
        coroutineScope.launch {
            updateChecker.checkForUpdates(showToast = true)
        }
    }

    LaunchedEffect(startDestination, env.sessions, env.currentSessionId) {
        if (
            startDestination == TerminalRoutes.TERMINAL_HOME_ROUTE &&
            env.sessions.isEmpty() &&
            env.currentSessionId == null
        ) {
            try {
                manager.createNewSession()
            } catch (_: Exception) {
            }
        }
    }

    // 使用 NavHost 处理所有导航
    NavHost(
        navController = navController,
        startDestination = if (startDestination != null) startDestination!! else TerminalRoutes.TERMINAL_HOME_ROUTE
    ) {

        composable(TerminalRoutes.TERMINAL_HOME_ROUTE) {
            CoryScaffold(
                env = env,
                onNavigateToSettings = {
                    navController.navigate(TerminalRoutes.SETTINGS_ROUTE)
                }
            )
        }

        composable(TerminalRoutes.SETUP_ROUTE) {
            SetupScreen(
                onBack = {
                    val sharedPreferences = context.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
                    navController.navigate(TerminalRoutes.TERMINAL_HOME_ROUTE) {
                        popUpTo(TerminalRoutes.SETUP_ROUTE) { inclusive = true }
                    }
                },
                onSetup = { commands ->
                    val sharedPreferences = context.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
                    env.onSetup(commands)
                    navController.navigate(TerminalRoutes.TERMINAL_HOME_ROUTE) {
                        popUpTo(TerminalRoutes.SETUP_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(TerminalRoutes.SETTINGS_ROUTE) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    // 当确定了目标页面后，导航到相应页面
    LaunchedEffect(startDestination) {
        if (startDestination != null && navController.currentBackStackEntry?.destination?.route != startDestination) {
            navController.navigate(startDestination!!) {
                popUpTo(TerminalRoutes.TERMINAL_HOME_ROUTE) { inclusive = true }
            }
        }
    }
}
