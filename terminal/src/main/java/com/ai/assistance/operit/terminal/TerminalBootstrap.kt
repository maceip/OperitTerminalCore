package com.ai.assistance.operit.terminal

import android.content.Context
import android.util.Log
import java.io.File

/**
 * One-time setup for the native shell environment.
 *
 * Creates the directory layout, symlinks bash/busybox from the native
 * library directory into PREFIX/bin, and writes a default .bashrc.
 *
 * Call [ensureEnvironment] from a background thread at app startup.
 * It is idempotent — safe to call on every launch.
 */
object TerminalBootstrap {

    private const val TAG = "TerminalBootstrap"

    /** Standard busybox applets to symlink. */
    private val BUSYBOX_APPLETS = listOf(
        "awk", "basename", "cat", "chmod", "chown", "clear", "cp",
        "cut", "date", "df", "diff", "dirname", "du", "echo", "env",
        "expr", "false", "find", "free", "grep", "gzip", "head",
        "id", "kill", "ln", "ls", "md5sum", "mkdir", "mktemp",
        "more", "mv", "nc", "od", "patch", "ping", "printf",
        "ps", "pwd", "readlink", "realpath", "rm", "rmdir", "sed",
        "seq", "sh", "sha256sum", "sleep", "sort", "stat", "strings",
        "tail", "tar", "tee", "test", "time", "touch", "tr", "true",
        "tty", "uname", "uniq", "unzip", "uptime", "vi", "wc",
        "wget", "which", "whoami", "xargs", "yes"
    )

    fun ensureEnvironment(context: Context) {
        val filesDir = context.filesDir
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val prefixDir = File(filesDir, "usr")
        val binDir = File(prefixDir, "bin")
        val libDir = File(prefixDir, "lib")
        val homeDir = File(filesDir, "home")
        val tmpDir = File(filesDir, "tmp")

        // Create directory structure
        listOf(binDir, libDir, homeDir, tmpDir).forEach { it.mkdirs() }

        // Link bash from native lib dir
        linkNativeBinary(nativeLibDir, binDir, "libbash.so", "bash")

        // Link busybox and create applet symlinks
        val busyboxPath = linkNativeBinary(nativeLibDir, binDir, "libbusybox.so", "busybox")
        if (busyboxPath != null) {
            createBusyboxSymlinks(busyboxPath, binDir)
        }

        // Write default .bashrc (if not already present)
        writeBashrc(homeDir)

        // Write default .profile for --login shells
        writeProfile(homeDir, binDir)

        Log.d(TAG, "Environment ready: prefix=$prefixDir")
    }

    /**
     * Symlink a native library (.so) into binDir with a human-readable name.
     * Returns the absolute path of the created link, or null on failure.
     */
    private fun linkNativeBinary(
        nativeLibDir: String,
        binDir: File,
        soName: String,
        linkName: String
    ): String? {
        val source = File(nativeLibDir, soName)
        val target = File(binDir, linkName)

        if (!source.exists()) {
            Log.w(TAG, "Native binary not found: $source")
            return null
        }

        // Re-create symlink if it doesn't point to the right place
        if (target.exists()) {
            try {
                if (target.canonicalPath == source.canonicalPath) {
                    return target.absolutePath
                }
            } catch (_: Exception) { }
            target.delete()
        }

        try {
            Runtime.getRuntime().exec(arrayOf("ln", "-sf", source.absolutePath, target.absolutePath)).waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to symlink $soName -> $linkName", e)
            return null
        }

        return target.absolutePath
    }

    private fun createBusyboxSymlinks(busyboxPath: String, binDir: File) {
        for (applet in BUSYBOX_APPLETS) {
            val link = File(binDir, applet)
            if (link.exists()) continue
            try {
                Runtime.getRuntime().exec(
                    arrayOf("ln", "-sf", busyboxPath, link.absolutePath)
                ).waitFor()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create busybox symlink: $applet", e)
            }
        }
    }

    private fun writeBashrc(homeDir: File) {
        val bashrc = File(homeDir, ".bashrc")
        if (bashrc.exists()) return

        bashrc.writeText(
            """
            # Cory shell environment
            export PS1='\[\e[1;32m\]\u@cory\[\e[0m\]:\[\e[1;34m\]\w\[\e[0m\]$ '
            alias ll='ls -lah --color=auto'
            alias la='ls -A --color=auto'
            alias l='ls --color=auto'
            export CLICOLOR=1
            """.trimIndent() + "\n"
        )
    }

    private fun writeProfile(homeDir: File, binDir: File) {
        val profile = File(homeDir, ".profile")
        if (profile.exists()) return

        profile.writeText(
            """
            # Source .bashrc for interactive login shells
            if [ -f "${'$'}HOME/.bashrc" ]; then
                . "${'$'}HOME/.bashrc"
            fi
            """.trimIndent() + "\n"
        )
    }
}
