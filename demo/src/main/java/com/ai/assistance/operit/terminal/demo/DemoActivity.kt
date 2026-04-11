package com.ai.assistance.operit.terminal.demo

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.terminal.Pty
import com.ai.assistance.operit.terminal.TerminalBootstrap
import com.ai.assistance.operit.terminal.service.TerminalService
import com.ai.assistance.operit.terminal.view.canvas.CanvasTerminalScreen
import com.ai.assistance.operit.terminal.view.canvas.RenderConfig
import com.ai.assistance.operit.terminal.view.canvas.TerminalTabRenderItem
import com.ai.assistance.operit.terminal.view.domain.ansi.AnsiTerminalEmulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, TerminalService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        setContent { FullTerminalDemo() }
    }
}

/**
 * Full-screen terminal demo showing all the upstream Canvas view components:
 * - Built-in tab bar with multiple tabs
 * - Close buttons on each tab
 * - "+" add tab button
 * - Tab switching
 * - Fullscreen mode with IME keyboard handling
 * - Scroll, pinch-to-zoom
 * - ANSI color rendering
 * - Cursor blinking
 *
 * Three shell sessions are pre-created so the tab bar is populated.
 * Type in any of them — they're real bash PTYs.
 */
@Composable
private fun FullTerminalDemo() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }

    // Three independent shell sessions
    data class ShellSession(
        val id: String,
        val title: String,
        val emulator: AnsiTerminalEmulator,
        var pty: Pty? = null,
        var writer: OutputStreamWriter? = null
    )

    val sessions = remember {
        listOf(
            ShellSession("1", "bash", AnsiTerminalEmulator(screenWidth = 120, screenHeight = 50, historySize = 1000)),
            ShellSession("2", "tools", AnsiTerminalEmulator(screenWidth = 120, screenHeight = 50, historySize = 1000)),
            ShellSession("3", "scratch", AnsiTerminalEmulator(screenWidth = 120, screenHeight = 50, historySize = 1000))
        )
    }
    var activeId by remember { mutableStateOf("1") }
    val activeSession = sessions.find { it.id == activeId } ?: sessions[0]

    // Bootstrap and launch all 3 shells
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            TerminalBootstrap.ensureEnvironment(context)
            val homeDir = File(context.filesDir, "home")
            val binDir = File(context.filesDir, "usr/bin")
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val env = mapOf(
                "HOME" to homeDir.absolutePath,
                "PATH" to "${binDir.absolutePath}:${nativeLibDir}:/system/bin",
                "PREFIX" to File(context.filesDir, "usr").absolutePath,
                "LD_LIBRARY_PATH" to "${File(context.filesDir, "usr/lib").absolutePath}:${nativeLibDir}",
                "TMPDIR" to File(context.filesDir, "tmp").absolutePath,
                "TERM" to "xterm-256color",
                "LANG" to "en_US.UTF-8",
                "SHELL" to File(binDir, "bash").absolutePath
            )

            for (session in sessions) {
                try {
                    val p = Pty.start(
                        command = arrayOf(File(binDir, "bash").absolutePath, "--login"),
                        environment = env,
                        workingDir = homeDir,
                        rows = 50,
                        cols = 120
                    )
                    session.pty = p
                    session.writer = OutputStreamWriter(p.stdin, Charsets.UTF_8)

                    // Read loop for each session
                    launch(Dispatchers.IO) {
                        val buf = ByteArray(4096)
                        try {
                            while (true) {
                                val n = p.stdout.read(buf)
                                if (n < 0) break
                                if (n > 0) {
                                    val text = String(buf, 0, n, Charsets.UTF_8)
                                    withContext(Dispatchers.Main) { session.emulator.parse(text) }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                } catch (e: Exception) {
                    Log.e("Demo", "Failed to start session ${session.id}", e)
                }
            }

            // Seed some color output in session 2
            sessions[1].writer?.let { w ->
                w.write("echo -e '\\033[1;32m=== Tools Available ===\\033[0m' && which bash busybox ls cat grep sed awk find sort 2>/dev/null && echo -e '\\033[1;36mBusybox applets:\\033[0m' && busybox --list 2>/dev/null | head -20\n")
                w.flush()
            }

            // Seed some content in session 3
            sessions[2].writer?.let { w ->
                w.write("echo -e '\\033[1;33m── System Info ──\\033[0m' && uname -a && echo && echo -e '\\033[1;35m── Environment ──\\033[0m' && env | sort | head -15\n")
                w.flush()
            }

            ready = true
        }
    }

    if (!ready) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF4EC9B0), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Text("Starting 3 shells...", color = Color(0xFF6B7D93), fontSize = 14.sp)
            }
        }
    } else {
        CanvasTerminalScreen(
            emulator = activeSession.emulator,
            modifier = Modifier.fillMaxSize(),
            config = RenderConfig(
                fontSize = 36f,
                backgroundColor = 0xFF0B1016.toInt(),
                foregroundColor = 0xFFD7E3F4.toInt(),
                cursorColor = 0xFF4EC9B0.toInt(),
                targetFps = 60
            ),
            pty = activeSession.pty,
            onInput = { input ->
                scope.launch(Dispatchers.IO) {
                    activeSession.writer?.let { it.write(input); it.flush() }
                }
            },
            tabs = sessions.map { s ->
                TerminalTabRenderItem(
                    id = s.id,
                    title = s.title,
                    canClose = s.id != "1" // First tab can't be closed
                )
            },
            currentTabId = activeId,
            onTabClick = { id -> activeId = id },
            onTabClose = { id ->
                // In demo, just switch away
                if (activeId == id) {
                    activeId = sessions.first { it.id != id }.id
                }
            },
            onNewTab = {
                // Demo doesn't create new tabs, but the "+" button will show
            }
        )
    }
}
