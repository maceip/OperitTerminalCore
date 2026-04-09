package com.ai.assistance.operit.terminal.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.terminal.Pty
import com.ai.assistance.operit.terminal.TerminalBootstrap
import com.ai.assistance.operit.terminal.view.canvas.CanvasTerminalScreen
import com.ai.assistance.operit.terminal.view.canvas.RenderConfig
import com.ai.assistance.operit.terminal.view.domain.ansi.AnsiTerminalEmulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter

/**
 * Minimal demo activity that launches a bash shell inside a PTY
 * and displays it using the CanvasTerminalView.
 *
 * This demonstrates the terminal library working independently —
 * no Cory runtimes, no proot, just bash + busybox.
 */
class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TerminalDemo() }
    }
}

@Composable
private fun ComponentActivity.TerminalDemo() {
    val context = this
    val scope = rememberCoroutineScope()

    val emulator = remember { AnsiTerminalEmulator(screenWidth = 80, screenHeight = 40, historySize = 500) }
    var pty by remember { mutableStateOf<Pty?>(null) }
    var writer by remember { mutableStateOf<OutputStreamWriter?>(null) }
    var statusText by remember { mutableStateOf("Bootstrapping environment…") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // 1. Set up directories, symlinks, .bashrc
            TerminalBootstrap.ensureEnvironment(context)

            // 2. Launch bash via PTY
            val homeDir = File(context.filesDir, "home")
            val binDir = File(context.filesDir, "usr/bin")
            val libDir = File(context.filesDir, "usr/lib")
            val nativeLibDir = context.applicationInfo.nativeLibraryDir

            val env = mapOf(
                "HOME" to homeDir.absolutePath,
                "PATH" to "${binDir.absolutePath}:${nativeLibDir}:/system/bin",
                "PREFIX" to File(context.filesDir, "usr").absolutePath,
                "LD_LIBRARY_PATH" to "${libDir.absolutePath}:${nativeLibDir}",
                "TMPDIR" to File(context.filesDir, "tmp").absolutePath,
                "TERM" to "xterm-256color",
                "LANG" to "en_US.UTF-8",
                "COLORTERM" to "truecolor",
                "SHELL" to File(binDir, "bash").absolutePath
            )

            try {
                val p = Pty.start(
                    command = arrayOf(File(binDir, "bash").absolutePath, "--login"),
                    environment = env,
                    workingDir = homeDir,
                    rows = 40,
                    cols = 80
                )
                pty = p
                writer = OutputStreamWriter(p.stdin, Charsets.UTF_8)
                statusText = ""

                // 3. Read output loop → feed into emulator
                val buf = ByteArray(4096)
                while (true) {
                    val n = p.stdout.read(buf)
                    if (n < 0) break
                    if (n > 0) {
                        val text = String(buf, 0, n, Charsets.UTF_8)
                        withContext(Dispatchers.Main) {
                            emulator.processInput(text)
                        }
                    }
                }
                statusText = "Shell exited"
            } catch (e: Exception) {
                Log.e("TerminalDemo", "PTY error", e)
                statusText = "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        if (statusText.isNotEmpty()) {
            Text(
                text = statusText,
                color = Color(0xFF4EC9B0),
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        CanvasTerminalScreen(
            emulator = emulator,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pty = pty,
            onInput = { input ->
                scope.launch(Dispatchers.IO) {
                    writer?.let {
                        it.write(input)
                        it.flush()
                    }
                }
            }
        )
    }
}
