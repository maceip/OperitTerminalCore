package com.ai.assistance.operit.terminal.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import com.ai.assistance.operit.terminal.TerminalEnv
import androidx.compose.ui.platform.LocalContext

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun CoryScaffold(
    env: TerminalEnv,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val terminalManager = remember(context) {
        com.ai.assistance.operit.terminal.TerminalManager.getInstance(context)
    }
    val agentUiState = rememberAgentUiState(terminalManager)
    val blocks = agentUiState.blocks(env.currentSessionId)
    val navigator = rememberSupportingPaneScaffoldNavigator()

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
        NavigableSupportingPaneScaffold(
            navigator = navigator,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1016))
                .padding(padding),
            mainPane = {
                AnimatedPane {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B1016))
                    ) {
                        AgentView(
                            sessionId = env.currentSessionId,
                            blocks = blocks,
                            onToggleText = { index ->
                                env.currentSessionId?.let { agentUiState.toggleText(it, index) }
                            },
                            onToggleCommand = { index ->
                                env.currentSessionId?.let { agentUiState.toggleCommand(it, index) }
                            }
                        )
                    }
                }
            },
            supportingPane = {
                AnimatedPane {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF11161C))
                    ) {
                        Text(
                            text = "Terminal (raw)",
                            color = Color(0xFFD7E3F4),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        TerminalPane(
                            env = env,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        )
    }
}
