package com.ai.assistance.operit.terminal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ai.assistance.operit.terminal.TerminalEnv
import com.ai.assistance.operit.terminal.utils.TerminalFontConfigManager
import com.ai.assistance.operit.terminal.view.canvas.CanvasTerminalScreen

@Composable
fun TerminalPane(
    env: TerminalEnv,
    modifier: Modifier = Modifier
) {
    val currentPty = remember(env.currentSessionId, env.sessions) {
        env.sessions.find { it.id == env.currentSessionId }?.pty
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val config = remember { TerminalFontConfigManager.getInstance(context).loadRenderConfig() }
    CanvasTerminalScreen(
        emulator = env.terminalEmulator,
        modifier = modifier,
        config = config,
        pty = currentPty,
        onInput = { env.onSendInput(it, false) },
        sessionId = env.currentSessionId,
        onScrollOffsetChanged = { id, offset -> env.saveScrollOffset(id, offset) },
        getScrollOffset = { id -> env.getScrollOffset(id) }
    )
}
