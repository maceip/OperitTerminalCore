package com.ai.assistance.operit.terminal.demo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.terminal.Pty
import com.ai.assistance.operit.terminal.TerminalBootstrap
import com.ai.assistance.operit.terminal.service.TerminalService
import com.ai.assistance.operit.terminal.ui.AgentBlock
import com.ai.assistance.operit.terminal.ui.AgentView
import com.ai.assistance.operit.terminal.ui.InputBar
import com.ai.assistance.operit.terminal.ui.TerminalPane
import com.ai.assistance.operit.terminal.view.canvas.CanvasTerminalScreen
import com.ai.assistance.operit.terminal.view.canvas.TerminalTabRenderItem
import com.ai.assistance.operit.terminal.view.domain.ansi.AnsiTerminalEmulator
import com.ai.assistance.operit.terminal.TerminalManager
import com.ai.assistance.operit.terminal.TerminalEnv
import com.ai.assistance.operit.terminal.rememberTerminalEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import androidx.compose.runtime.mutableStateListOf

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, TerminalService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent { DemoApp() }
    }
}

// ─── Color palette ───
private val BgDark = Color(0xFF0B1016)
private val BgCard = Color(0xFF11161C)
private val BgTab = Color(0xFF1A2332)
private val BgTabActive = Color(0xFF1F7A4D)
private val TextPrimary = Color(0xFFD7E3F4)
private val TextSecondary = Color(0xFF6B7D93)
private val AccentGreen = Color(0xFF4EC9B0)

@Composable
private fun DemoApp() {
    var currentScreen by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Screen selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E141B))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Terminal", "Agent View", "Dual Pane").forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index == currentScreen) BgTabActive else BgTab)
                        .clickable { currentScreen = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (index == currentScreen) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Screen content
        when (currentScreen) {
            0 -> ScreenFullTerminal()
            1 -> ScreenAgentView()
            2 -> ScreenDualPane()
        }
    }
}

// ═══════════════════════════════════════════
//  Screen 1: Full-screen Canvas terminal
// ═══════════════════════════════════════════

@Composable
private fun ScreenFullTerminal() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val emulator = remember { AnsiTerminalEmulator(screenWidth = 80, screenHeight = 40, historySize = 500) }
    var pty by remember { mutableStateOf<Pty?>(null) }
    var writer by remember { mutableStateOf<OutputStreamWriter?>(null) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            TerminalBootstrap.ensureEnvironment(context)
            val homeDir = File(context.filesDir, "home")
            val binDir = File(context.filesDir, "usr/bin")
            val nativeLibDir = context.applicationInfo.nativeLibraryDir

            try {
                val p = Pty.start(
                    command = arrayOf(File(binDir, "bash").absolutePath, "--login"),
                    environment = mapOf(
                        "HOME" to homeDir.absolutePath,
                        "PATH" to "${binDir.absolutePath}:${nativeLibDir}:/system/bin",
                        "PREFIX" to File(context.filesDir, "usr").absolutePath,
                        "LD_LIBRARY_PATH" to "${File(context.filesDir, "usr/lib").absolutePath}:${nativeLibDir}",
                        "TMPDIR" to File(context.filesDir, "tmp").absolutePath,
                        "TERM" to "xterm-256color",
                        "LANG" to "en_US.UTF-8",
                        "SHELL" to File(binDir, "bash").absolutePath
                    ),
                    workingDir = homeDir,
                    rows = 40,
                    cols = 80
                )
                pty = p
                writer = OutputStreamWriter(p.stdin, Charsets.UTF_8)
                ready = true

                val buf = ByteArray(4096)
                while (true) {
                    val n = p.stdout.read(buf)
                    if (n < 0) break
                    if (n > 0) {
                        val text = String(buf, 0, n, Charsets.UTF_8)
                        withContext(Dispatchers.Main) { emulator.parse(text) }
                    }
                }
            } catch (e: Exception) {
                Log.e("Demo", "PTY error", e)
            }
        }
    }

    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AccentGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Text("Starting shell...", color = TextSecondary, fontSize = 14.sp)
            }
        }
    } else {
        CanvasTerminalScreen(
            emulator = emulator,
            modifier = Modifier.fillMaxSize(),
            pty = pty,
            onInput = { input ->
                scope.launch(Dispatchers.IO) {
                    writer?.let { it.write(input); it.flush() }
                }
            },
            tabs = listOf(
                TerminalTabRenderItem(id = "1", title = "bash", canClose = false),
            ),
            currentTabId = "1",
            onNewTab = { /* demo: no-op */ }
        )
    }
}

// ═══════════════════════════════════════════
//  Screen 2: Parsed Agent View
// ═══════════════════════════════════════════

@Composable
private fun ScreenAgentView() {
    val blocks = remember {
        mutableStateListOf<AgentBlock>(
            AgentBlock.UserPrompt("clone https://github.com/acme/webapp and get it running"),

            AgentBlock.AgentText(
                text = "I'll clone the repository and set it up for you. Let me start by cloning " +
                    "the repo, then I'll install dependencies and start the dev server. This should " +
                    "only take a minute or two depending on the project size and number of dependencies.\n\n" +
                    "Let me also check if there are any specific setup instructions in the README.",
                isCollapsed = true
            ),

            AgentBlock.Command(
                command = "git clone https://github.com/acme/webapp",
                output = "Cloning into 'webapp'...\nremote: Enumerating objects: 847, done.\nremote: Counting objects: 100% (847/847), done.\nresolving deltas: 100% (312/312), done.",
                exitCode = 0,
                durationMs = 3200,
                isExpanded = false
            ),

            AgentBlock.Command(
                command = "npm install",
                output = "added 847 packages, and audited 848 packages in 34s\n\n109 packages are looking for funding\n  run `npm fund` for details\n\n12 vulnerabilities (1 low, 6 moderate, 5 high)\n\nTo address issues that do not require attention, run:\n  npm audit fix",
                exitCode = 0,
                durationMs = 34000,
                isExpanded = false
            ),

            AgentBlock.Command(
                command = "npm run dev",
                output = "VITE v5.4.1 ready in 312ms\n\n  ➜  Local:   http://localhost:3000\n  ➜  Network: http://192.168.1.42:3000",
                exitCode = 0,
                durationMs = 2100,
                isExpanded = false
            ),

            AgentBlock.AgentText(
                text = "The app is running at localhost:3000. I found 12 npm vulnerabilities but none are critical. Want me to fix them?",
                isCollapsed = false
            ),

            AgentBlock.UserPrompt("run the tests"),

            AgentBlock.Command(
                command = "npm test",
                output = "FAIL src/App.test.tsx\n  ● renders app heading\n\n    TypeError: Cannot read properties of undefined (reading 'title')\n\n      14 |   render(<App />);\n      15 |   const heading = screen.getByRole('heading');\n    > 16 |   expect(heading).toHaveTextContent(config.title);\n\nTests: 1 failed, 22 passed, 23 total",
                exitCode = 1,
                durationMs = 4200,
                isExpanded = true
            ),

            AgentBlock.AgentText(
                text = "One test failed — App.test.tsx is referencing config.title which was removed in a recent refactor. Let me fix it.",
                isCollapsed = false
            ),

            AgentBlock.Diff(
                filePath = "src/App.test.tsx",
                hunks = listOf(
                    com.ai.assistance.operit.terminal.ui.DiffHunk(
                        header = "@@ -14,7 +14,7 @@",
                        lines = listOf(
                            "   render(<App />);",
                            "   const heading = screen.getByRole('heading');",
                            "-  expect(heading).toHaveTextContent(config.title);",
                            "+  expect(heading).toBeInTheDocument();",
                        )
                    )
                )
            ),

            AgentBlock.Command(
                command = "npm test",
                output = "PASS src/App.test.tsx\nTests: 23 passed, 23 total\nTime:  1.847s",
                exitCode = 0,
                durationMs = 1847,
                isExpanded = false
            ),

            AgentBlock.AgentText(
                text = "All 23 tests passing now. The fix was straightforward — the test was asserting against a config property that no longer exists after the refactor.",
                isCollapsed = false
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        AgentView(
            sessionId = "demo",
            blocks = blocks,
            onToggleText = { index ->
                val block = blocks[index]
                if (block is AgentBlock.AgentText) {
                    blocks[index] = block.copy(isCollapsed = !block.isCollapsed)
                }
            },
            onToggleCommand = { index ->
                val block = blocks[index]
                if (block is AgentBlock.Command) {
                    blocks[index] = block.copy(isExpanded = !block.isExpanded)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
//  Screen 3: Dual Pane (agent + terminal)
// ═══════════════════════════════════════════

@Composable
private fun ScreenDualPane() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val emulator = remember { AnsiTerminalEmulator(screenWidth = 80, screenHeight = 40, historySize = 500) }
    var pty by remember { mutableStateOf<Pty?>(null) }
    var writer by remember { mutableStateOf<OutputStreamWriter?>(null) }
    var ready by remember { mutableStateOf(false) }

    val blocks = remember {
        mutableStateListOf<AgentBlock>(
            AgentBlock.UserPrompt("show me the project structure"),
            AgentBlock.Command(
                command = "ls -la",
                output = "total 48\ndrwxr-xr-x  8 user user 4096 Apr 10 src/\ndrwxr-xr-x  3 user user 4096 Apr 10 public/\n-rw-r--r--  1 user user  847 Apr 10 package.json\n-rw-r--r--  1 user user  234 Apr 10 vite.config.ts",
                exitCode = 0,
                durationMs = 12,
                isExpanded = true
            ),
            AgentBlock.Command(
                command = "rg \"export default\" src/ --count",
                output = "src/App.tsx:1\nsrc/main.tsx:1\nsrc/hooks/useAuth.ts:1",
                exitCode = 0,
                durationMs = 8,
                isExpanded = true
            ),
            AgentBlock.AgentText(
                text = "The project has a standard Vite + React structure with 3 main exports.",
                isCollapsed = false
            )
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            TerminalBootstrap.ensureEnvironment(context)
            val homeDir = File(context.filesDir, "home")
            val binDir = File(context.filesDir, "usr/bin")
            val nativeLibDir = context.applicationInfo.nativeLibraryDir

            try {
                val p = Pty.start(
                    command = arrayOf(File(binDir, "bash").absolutePath, "--login"),
                    environment = mapOf(
                        "HOME" to homeDir.absolutePath,
                        "PATH" to "${binDir.absolutePath}:${nativeLibDir}:/system/bin",
                        "PREFIX" to File(context.filesDir, "usr").absolutePath,
                        "LD_LIBRARY_PATH" to "${File(context.filesDir, "usr/lib").absolutePath}:${nativeLibDir}",
                        "TMPDIR" to File(context.filesDir, "tmp").absolutePath,
                        "TERM" to "xterm-256color",
                        "LANG" to "en_US.UTF-8",
                        "SHELL" to File(binDir, "bash").absolutePath
                    ),
                    workingDir = homeDir,
                    rows = 40,
                    cols = 80
                )
                pty = p
                writer = OutputStreamWriter(p.stdin, Charsets.UTF_8)
                ready = true

                val buf = ByteArray(4096)
                while (true) {
                    val n = p.stdout.read(buf)
                    if (n < 0) break
                    if (n > 0) {
                        val text = String(buf, 0, n, Charsets.UTF_8)
                        withContext(Dispatchers.Main) { emulator.parse(text) }
                    }
                }
            } catch (e: Exception) {
                Log.e("Demo", "Dual pane PTY error", e)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Left: Agent parsed view (60%)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(BgDark)
        ) {
            Text(
                text = "Agent View",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AgentView(
                sessionId = "dual-demo",
                blocks = blocks,
                onToggleText = { index ->
                    val block = blocks[index]
                    if (block is AgentBlock.AgentText) {
                        blocks[index] = block.copy(isCollapsed = !block.isCollapsed)
                    }
                },
                onToggleCommand = { index ->
                    val block = blocks[index]
                    if (block is AgentBlock.Command) {
                        blocks[index] = block.copy(isExpanded = !block.isExpanded)
                    }
                }
            )
        }

        // Right: Raw terminal (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(BgCard)
        ) {
            Text(
                text = "Terminal",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (!ready) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen, modifier = Modifier.size(24.dp))
                }
            } else {
                CanvasTerminalScreen(
                    emulator = emulator,
                    modifier = Modifier.fillMaxSize(),
                    pty = pty,
                    onInput = { input ->
                        scope.launch(Dispatchers.IO) {
                            writer?.let { it.write(input); it.flush() }
                        }
                    }
                )
            }
        }
    }
}
