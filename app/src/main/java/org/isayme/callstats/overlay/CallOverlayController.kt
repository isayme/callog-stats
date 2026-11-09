package org.isayme.callstats.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.isayme.callstats.ui.CallStatsTheme

class CallOverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val density = appContext.resources.displayMetrics.density

    private var composeView: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null
    private var uiState by mutableStateOf<OverlayUiState?>(null)

    val isShowing: Boolean get() = composeView != null

    fun show(state: OverlayUiState) {
        uiState = state
        if (composeView != null) return

        val newOwner = OverlayOwner()
        newOwner.create()

        val view = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(newOwner)
            setViewTreeSavedStateRegistryOwner(newOwner)
            setViewTreeViewModelStoreOwner(newOwner)
            setContent {
                CallStatsTheme(darkTheme = true) {
                    val state = uiState
                    if (state != null) {
                        CallOverlayCard(
                            state = state,
                            onToggleCollapsed = {
                                update { current -> current.copy(collapsed = !current.collapsed) }
                            },
                            onDrag = ::dragBy,
                        )
                    }
                }
            }
        }

        val host = OverlayAccessibilityService.current()
        val hostContext = host ?: appContext
        val manager = hostContext.getSystemService(WindowManager::class.java) ?: run {
            newOwner.destroy()
            return
        }
        val params = buildLayoutParams(
            type = if (host != null) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            },
        )
        val added = runCatching { manager.addView(view, params) }.isSuccess
        if (!added) {
            newOwner.destroy()
            return
        }
        newOwner.resume()

        composeView = view
        owner = newOwner
        layoutParams = params
        windowManager = manager
    }

    fun update(transform: (OverlayUiState) -> OverlayUiState) {
        val current = uiState ?: return
        uiState = transform(current)
    }

    fun setCollapsed(collapsed: Boolean) {
        update { current -> current.copy(collapsed = collapsed) }
    }

    fun hide() {
        val view = composeView ?: return
        composeView = null
        layoutParams = null
        uiState = null
        runCatching { windowManager?.removeView(view) }
        windowManager = null
        owner?.destroy()
        owner = null
    }

    private fun dragBy(dx: Float, dy: Float) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val manager = windowManager ?: return
        val metrics = appContext.resources.displayMetrics

        val halfScreenWidth = metrics.widthPixels / 2
        val minX = -halfScreenWidth + view.width / 2
        val maxX = halfScreenWidth - view.width / 2
        val maxY = (metrics.heightPixels - view.height).coerceAtLeast(0)

        params.x = (params.x + dx.toInt()).coerceIn(minX, maxX)
        params.y = (params.y + dy.toInt()).coerceIn(0, maxY)
        runCatching { manager.updateViewLayout(view, params) }
    }

    @Suppress("DEPRECATION")
    private fun buildLayoutParams(type: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (64 * density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry
        override val viewModelStore: ViewModelStore = ViewModelStore()

        fun create() {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun resume() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}
