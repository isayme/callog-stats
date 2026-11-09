package org.isayme.callstats.overlay

import android.accessibilityservice.AccessibilityService
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Hosts call-stat overlays as accessibility overlays.
 *
 * MIUI hides `TYPE_APPLICATION_OVERLAY` windows on the lock screen even when the
 * "show on lock screen" permission is granted. Windows of type
 * `TYPE_ACCESSIBILITY_OVERLAY` are managed by the accessibility framework and are
 * allowed above the keyguard, so the controller prefers this service whenever it
 * is enabled. No accessibility events are consumed; the service only provides a
 * window token.
 */
class OverlayAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        clear(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clear(this)
        super.onDestroy()
    }

    /** Window manager bound to this service's accessibility window token. */
    fun accessibilityWindowManager(): WindowManager? =
        getSystemService(WindowManager::class.java)

    companion object {
        @Volatile
        private var active: OverlayAccessibilityService? = null

        fun isConnected(): Boolean = active != null

        fun current(): OverlayAccessibilityService? = active

        private fun clear(service: OverlayAccessibilityService) {
            if (active === service) active = null
        }
    }
}
