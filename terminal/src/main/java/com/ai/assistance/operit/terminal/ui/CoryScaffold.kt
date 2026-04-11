package com.ai.assistance.operit.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ai.assistance.operit.terminal.TerminalEnv

/**
 * Shell-first scaffold: terminal is the primary view on all form factors.
 *
 * The terminal canvas fills the main area. The InputBar at the bottom
 * acts as the command entry. Session tabs at top for multi-session.
 *
 * Agent/structured views can be layered on later once the shell is solid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoryScaffold(
    env: TerminalEnv,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF0B1016),
        topBar = {
            SessionTabs(
                sessions = env.sessions,
                currentSessionId = env.currentSessionId,
                onSessionClick = env::onSwitchSession,
                onAddSession = env::onNewSession,
                onSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            InputBar(env)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1016))
                .padding(padding)
        ) {
            TerminalPane(
                env = env,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
