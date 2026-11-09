package org.isayme.callstats.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.isayme.callstats.overlay.OverlayAccessibilityService

data class PermissionState(
    val callLog: Boolean,
    val phoneState: Boolean,
    val overlay: Boolean,
    val accessibility: Boolean,
    val notifications: Boolean,
    val ignoringBatteryOptimizations: Boolean,
) {
    val callAccessGranted: Boolean get() = callLog && phoneState

    /** Either overlay path is enough to render statistics. */
    val overlayAvailable: Boolean get() = overlay || accessibility
    val readyForMonitoring: Boolean get() = callAccessGranted && overlayAvailable

    companion object {
        fun current(context: Context): PermissionState = PermissionState(
            callLog = context.isGranted(Manifest.permission.READ_CALL_LOG),
            phoneState = context.isGranted(Manifest.permission.READ_PHONE_STATE),
            overlay = Settings.canDrawOverlays(context),
            accessibility = context.isAccessibilityServiceEnabled(),
            notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.isGranted(Manifest.permission.POST_NOTIFICATIONS),
            ignoringBatteryOptimizations = context.isIgnoringBatteryOptimizations(),
        )

        fun runtimePermissionsToRequest(context: Context): Array<String> {
            val permissions = mutableListOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !context.isGranted(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                permissions += Manifest.permission.POST_NOTIFICATIONS
            }
            return permissions.toTypedArray()
        }

        fun overlaySettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        /**
         * Opens the system accessibility list. MIUI exposes the service there,
         * and enabling it lets the overlay render above the lock screen.
         */
        fun accessibilitySettingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        @SuppressLint("BatteryLife") // Sideloaded build: the exemption keeps call monitoring alive.
        fun batteryOptimizationIntent(context: Context): Intent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun appDetailsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.isAccessibilityServiceEnabled(): Boolean {
    if (OverlayAccessibilityService.isConnected()) return true
    val enabled = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val target = ComponentName(this, OverlayAccessibilityService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == target }
}

object OemSettings {

    private val autoStartComponents = listOf(
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
        ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        ),
        ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        ),
    )

    fun autoStartIntent(context: Context): Intent? {
        val packageManager = context.packageManager
        return autoStartComponents
            .asSequence()
            .map { component -> Intent().setComponent(component) }
            .firstOrNull { intent -> intent.resolveActivity(packageManager) != null }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun isLikelyMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            brand.contains("redmi") || brand.contains("poco")
    }
}
