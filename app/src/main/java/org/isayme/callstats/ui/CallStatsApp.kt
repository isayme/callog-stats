package org.isayme.callstats.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.isayme.callstats.call.CallMonitorService
import org.isayme.callstats.permissions.OemSettings
import org.isayme.callstats.permissions.PermissionState
import org.isayme.callstats.settings.AppSettings
import org.isayme.callstats.settings.SettingsRepository

@Composable
fun CallStatsApp(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissions by remember { mutableStateOf(PermissionState.current(context)) }
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissions = PermissionState.current(context)
    }
    val specialPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        permissions = PermissionState.current(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = PermissionState.current(context)
    }

    LaunchedEffect(settings.monitoringEnabled, permissions.readyForMonitoring) {
        if (settings.monitoringEnabled && permissions.readyForMonitoring) {
            runCatching { CallMonitorService.start(context) }
        }
    }

    fun setMonitoring(enabled: Boolean) {
        scope.launch {
            settingsRepository.setMonitoringEnabled(enabled)
            if (enabled) {
                runCatching { CallMonitorService.start(context) }
            } else {
                CallMonitorService.stop(context)
            }
        }
    }

    fun requestRuntimePermissions() {
        runtimePermissionLauncher.launch(PermissionState.runtimePermissionsToRequest(context))
    }

    fun requestOverlayPermission() {
        val intent = PermissionState.overlaySettingsIntent(context)
        runCatching { specialPermissionLauncher.launch(intent) }
            .onFailure { runCatching { context.startActivity(PermissionState.appDetailsIntent(context)) } }
    }

    fun requestBatteryOptimizationExemption() {
        val intent = PermissionState.batteryOptimizationIntent(context)
        runCatching { specialPermissionLauncher.launch(intent) }
            .onFailure { runCatching { context.startActivity(PermissionState.appDetailsIntent(context)) } }
    }

    fun requestAccessibility() {
        val intent = PermissionState.accessibilitySettingsIntent()
        runCatching { specialPermissionLauncher.launch(intent) }
            .onFailure { runCatching { context.startActivity(intent) } }
    }

    fun openAutoStartSettings() {
        val intent = OemSettings.autoStartIntent(context) ?: PermissionState.appDetailsIntent(context)
        runCatching { context.startActivity(intent) }
    }

    fun previewOverlay() {
        if (!permissions.overlayAvailable) {
            requestOverlayPermission()
            return
        }
        runCatching { CallMonitorService.preview(context) }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onBack = { showSettings = false },
            onDuplicateWindowChange = { minutes ->
                scope.launch { settingsRepository.setDuplicateWindowMinutes(minutes) }
            },
            onLookbackMonthsChange = { months ->
                scope.launch { settingsRepository.setLookbackMonths(months) }
            },
            onNormalizeNumbersChange = { enabled ->
                scope.launch { settingsRepository.setNormalizeNumbers(enabled) }
            },
            onShowOnOutgoingChange = { enabled ->
                scope.launch { settingsRepository.setShowOnOutgoing(enabled) }
            },
        )
    } else {
        HomeScreen(
            permissions = permissions,
            settings = settings,
            onToggleMonitoring = ::setMonitoring,
            onRequestRuntimePermissions = ::requestRuntimePermissions,
            onRequestOverlayPermission = ::requestOverlayPermission,
            onRequestAccessibility = ::requestAccessibility,
            onRequestBatteryOptimization = ::requestBatteryOptimizationExemption,
            onOpenAutoStartSettings = ::openAutoStartSettings,
            onPreviewOverlay = ::previewOverlay,
            onOpenSettings = { showSettings = true },
        )
    }
}
