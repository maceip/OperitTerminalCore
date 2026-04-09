package com.ai.assistance.operit.terminal.provider.type

import android.content.Context
import android.util.Log
import com.ai.assistance.operit.terminal.Pty
import com.ai.assistance.operit.terminal.TerminalSession
import com.ai.assistance.operit.terminal.data.SSHAuthType
import com.ai.assistance.operit.terminal.data.SSHConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * SSH terminal provider.
 *
 * Launches a local bash shell via PTY and then executes an ssh command
 * to connect to the remote server.
 */
class SSHTerminalProvider(
    private val context: Context,
    private val sshConfig: SSHConfig
) : TerminalProvider {

    private val activeSessions = ConcurrentHashMap<String, TerminalSession>()

    companion object {
        private const val TAG = "SSHTerminalProvider"
    }

    override suspend fun isConnected(): Boolean = activeSessions.isNotEmpty()

    override suspend fun connect(): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() {
        activeSessions.keys.toList().forEach { closeSession(it) }
    }

    override suspend fun startSession(sessionId: String): Result<Pair<TerminalSession, Pty>> {
        return withContext(Dispatchers.IO) {
            try {
                val filesDir = context.filesDir
                val binDir = File(filesDir, "usr/bin")
                val homeDir = File(filesDir, "home")
                val bash = File(binDir, "bash").absolutePath

                val sshCommand = buildSshCommand()
                val command = arrayOf(bash, "-c", sshCommand)
                val env = buildEnvironment()

                Log.d(TAG, "Starting SSH session: $sshCommand")

                val pty = Pty.start(command, env, homeDir)

                val session = TerminalSession(
                    process = pty.process,
                    stdout = pty.stdout,
                    stdin = pty.stdin
                )

                activeSessions[sessionId] = session
                Result.success(Pair(session, pty))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SSH session", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun closeSession(sessionId: String) {
        activeSessions.remove(sessionId)?.let { session ->
            session.process.destroy()
            Log.d(TAG, "Closed SSH session: $sessionId")
        }
    }

    override suspend fun executeHiddenCommand(
        command: String,
        executorKey: String,
        timeoutMs: Long
    ): HiddenExecResult {
        return HiddenExecResult(
            output = "",
            exitCode = -1,
            state = HiddenExecResult.State.EXECUTION_ERROR,
            error = "Hidden command execution not yet supported for SSH"
        )
    }

    override suspend fun getWorkingDirectory(): String = "~"

    override fun getEnvironment(): Map<String, String> = mapOf(
        "TERM" to "xterm-256color",
        "LANG" to "en_US.UTF-8"
    )

    private fun buildSshCommand(): String {
        val cmd = StringBuilder()

        if (sshConfig.authType == SSHAuthType.PASSWORD && sshConfig.password != null) {
            cmd.append("sshpass -p '${sshConfig.password}' ")
        }

        cmd.append("ssh")
        cmd.append(" -p ${sshConfig.port}")

        if (sshConfig.authType == SSHAuthType.PUBLIC_KEY && sshConfig.privateKeyPath != null) {
            cmd.append(" -i \"${sshConfig.privateKeyPath}\"")
        }

        cmd.append(" -o StrictHostKeyChecking=no")

        if (sshConfig.enableKeepAlive) {
            cmd.append(" -o ServerAliveInterval=${sshConfig.keepAliveInterval}")
            cmd.append(" -o ServerAliveCountMax=3")
        }

        cmd.append(" ${sshConfig.username}@${sshConfig.host}")
        return cmd.toString()
    }

    private fun buildEnvironment(): Map<String, String> {
        val filesDir = context.filesDir
        val prefixDir = File(filesDir, "usr")
        val binDir = File(prefixDir, "bin")
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        return mapOf(
            "PATH" to "${binDir.absolutePath}:${nativeLibDir}:/system/bin",
            "HOME" to File(filesDir, "home").absolutePath,
            "PREFIX" to prefixDir.absolutePath,
            "LD_LIBRARY_PATH" to "${nativeLibDir}:${binDir.absolutePath}",
            "TMPDIR" to File(filesDir, "tmp").absolutePath,
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8"
        )
    }
}
