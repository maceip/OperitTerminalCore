package com.ai.assistance.operit.terminal.view.canvas

import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ai.assistance.operit.terminal.view.domain.ansi.AnsiTerminalEmulator

/**
 * Compose wrapper for the Canvas terminal renderer.
 *
 * Tab management is handled by our Compose SessionTabs — the internal
 * Canvas tab bar is disabled by passing empty tab state.
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
    getScrollOffset: ((String) -> Float)? = null
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
                // No tab bar — tabs are handled by Compose SessionTabs
                setTabBarState(emptyList(), null, null, null, null)

                post { requestFocus() }

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
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
        },
        onRelease = { view ->
            view.release()
        },
        modifier = modifier
    )
}
