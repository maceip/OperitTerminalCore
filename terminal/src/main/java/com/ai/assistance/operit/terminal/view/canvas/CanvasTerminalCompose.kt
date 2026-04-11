package com.ai.assistance.operit.terminal.view.canvas

import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ai.assistance.operit.terminal.view.domain.ansi.AnsiTerminalEmulator

/**
 * Compose
 * CanvasTerminalViewCompose
 */
@Composable
fun CanvasTerminalScreen(
    emulator: AnsiTerminalEmulator,
    modifier: Modifier = Modifier,
    config: RenderConfig = RenderConfig(),
    pty: com.ai.assistance.operit.terminal.Pty? = null,
    imeAnimationOffsetPx: Int = 0,
    committedImeBottomInsetPx: Int = 0,
    onInput: (String) -> Unit = {},
    onScaleChanged: (Float) -> Unit = {},
    sessionId: String? = null,
    onScrollOffsetChanged: ((String, Float) -> Unit)? = null,
    getScrollOffset: ((String) -> Float)? = null,
    tabs: List<TerminalTabRenderItem> = emptyList(),
    currentTabId: String? = null,
    onTabClick: ((String) -> Unit)? = null,
    onTabClose: ((String) -> Unit)? = null,
    onNewTab: (() -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            CanvasTerminalView(context).apply {
                setConfig(config)
                setEmulator(emulator)
                setPty(pty)
                setImeViewportState(
                    animationOffsetPx = imeAnimationOffsetPx,
                    committedBottomInsetPx = committedImeBottomInsetPx
                )
                setInputCallback(onInput)
                setScaleCallback(onScaleChanged)
                setSessionScrollCallbacks(sessionId, onScrollOffsetChanged, getScrollOffset)
                setTabBarState(tabs, currentTabId, onTabClick, onTabClose, onNewTab)

                //
                post {
                    requestFocus()
                }

                // ，
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false //  false  View
                }
            }
        },
        update = { view ->
            view.setConfig(config)
            view.setEmulator(emulator)
            view.setPty(pty)
            view.setImeViewportState(
                animationOffsetPx = imeAnimationOffsetPx,
                committedBottomInsetPx = committedImeBottomInsetPx
            )
            view.setInputCallback(onInput)
            view.setSessionScrollCallbacks(sessionId, onScrollOffsetChanged, getScrollOffset)
            view.setTabBarState(tabs, currentTabId, onTabClick, onTabClose, onNewTab)
        },
        onRelease = { view ->
            // ，
            view.release()
        },
        modifier = modifier
    )
}

/**
 * Canvas
 */
@Composable
fun ConfigurableCanvasTerminal(
    emulator: AnsiTerminalEmulator,
    modifier: Modifier = Modifier,
    fontSize: Float = 14f,
    backgroundColor: Int = 0xFF000000.toInt(),
    foregroundColor: Int = 0xFFFFFFFF.toInt(),
    cursorColor: Int = 0xFF00FF00.toInt(),
    onInput: (String) -> Unit = {}
) {
    val config = remember(fontSize, backgroundColor, foregroundColor, cursorColor) {
        RenderConfig(
            fontSize = fontSize,
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            cursorColor = cursorColor
        )
    }

    var currentScale by remember { mutableStateOf(1f) }

    CanvasTerminalScreen(
        emulator = emulator,
        modifier = modifier,
        config = config,
        onInput = onInput,
        onScaleChanged = { scale -> currentScale = scale }
    )
}

/**
 * Canvas
 */
@Composable
fun PerformanceMonitoredTerminal(
    emulator: AnsiTerminalEmulator,
    modifier: Modifier = Modifier,
    config: RenderConfig = RenderConfig(),
    onInput: (String) -> Unit = {},
    onFpsUpdate: (Float) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            CanvasTerminalView(context).apply {
                setConfig(config)
                setEmulator(emulator)
                setInputCallback(onInput)
                setPerformanceCallback { fps: Float, frameTime: Long ->
                    onFpsUpdate(fps)
                }

                // ，
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false //  false  View
                }
            }
        },
        update = { view ->
            view.setConfig(config)
            view.setEmulator(emulator)
        },
        onRelease = { view ->
            // ，
            view.release()
        },
        modifier = modifier
    )
}

/**
 * Canvas
 * ，
 */
@Composable
fun CanvasTerminalOutput(
    emulator: AnsiTerminalEmulator,
    modifier: Modifier = Modifier,
    config: RenderConfig = RenderConfig(),
    pty: com.ai.assistance.operit.terminal.Pty? = null,
    imeAnimationOffsetPx: Int = 0,
    committedImeBottomInsetPx: Int = 0,
    onRequestShowKeyboard: (() -> Unit)? = null,
    sessionId: String? = null,
    onScrollOffsetChanged: ((String, Float) -> Unit)? = null,
    getScrollOffset: ((String) -> Float)? = null,
    tabs: List<TerminalTabRenderItem> = emptyList(),
    currentTabId: String? = null,
    onTabClick: ((String) -> Unit)? = null,
    onTabClose: ((String) -> Unit)? = null,
    onNewTab: (() -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            CanvasTerminalView(context).apply {
                setConfig(config)
                setEmulator(emulator)
                setPty(pty)
                setImeViewportState(
                    animationOffsetPx = imeAnimationOffsetPx,
                    committedBottomInsetPx = committedImeBottomInsetPx
                )
                setFullscreenMode(false) // ：
                setOnRequestShowKeyboard(onRequestShowKeyboard)
                setSessionScrollCallbacks(sessionId, onScrollOffsetChanged, getScrollOffset)
                setTabBarState(tabs, currentTabId, onTabClick, onTabClose, onNewTab)

                // ，
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false //  false  View
                }
            }
        },
        update = { view ->
            view.setConfig(config)
            view.setEmulator(emulator)
            view.setPty(pty)
            view.setImeViewportState(
                animationOffsetPx = imeAnimationOffsetPx,
                committedBottomInsetPx = committedImeBottomInsetPx
            )
            view.setOnRequestShowKeyboard(onRequestShowKeyboard)
            view.setSessionScrollCallbacks(sessionId, onScrollOffsetChanged, getScrollOffset)
            view.setTabBarState(tabs, currentTabId, onTabClick, onTabClose, onNewTab)
        },
        onRelease = { view ->
            // ，
            view.release()
        },
        modifier = modifier
    )
}

