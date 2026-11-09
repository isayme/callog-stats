package org.isayme.callstats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.isayme.callstats.R
import org.isayme.callstats.permissions.OemSettings
import org.isayme.callstats.permissions.PermissionState
import org.isayme.callstats.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    permissions: PermissionState,
    settings: AppSettings,
    onToggleMonitoring: (Boolean) -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onPreviewOverlay: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StatusCard(
                permissions = permissions,
                settings = settings,
                onToggleMonitoring = onToggleMonitoring,
            )

            PermissionsCard(
                permissions = permissions,
                onRequestRuntimePermissions = onRequestRuntimePermissions,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestAccessibility = onRequestAccessibility,
                onRequestBatteryOptimization = onRequestBatteryOptimization,
            )

            if (OemSettings.isLikelyMiui()) {
                AutoStartCard(onOpenAutoStartSettings = onOpenAutoStartSettings)
            }

            OutlinedButton(
                onClick = onPreviewOverlay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.preview_overlay))
            }

            Text(
                text = stringResource(R.string.privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(
    permissions: PermissionState,
    settings: AppSettings,
    onToggleMonitoring: (Boolean) -> Unit,
) {
    val running = settings.monitoringEnabled
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (running) MaterialTheme.colorScheme.primary else Color(0xFF9AA39D),
                            shape = CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (running) R.string.status_running else R.string.status_stopped,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            if (running) R.string.status_running_desc else R.string.status_stopped_desc,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onToggleMonitoring(!running) },
                enabled = running || permissions.readyForMonitoring,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (running) R.string.action_stop else R.string.action_start,
                    ),
                )
            }

            if (!permissions.readyForMonitoring) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.status_need_permissions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    permissions: PermissionState,
    onRequestRuntimePermissions: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))

            PermissionRow(
                title = stringResource(R.string.permission_call_log),
                description = stringResource(R.string.permission_call_log_desc),
                granted = permissions.callAccessGranted,
                onRequest = onRequestRuntimePermissions,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            PermissionRow(
                title = stringResource(R.string.permission_overlay),
                description = stringResource(R.string.permission_overlay_desc),
                granted = permissions.overlay,
                onRequest = onRequestOverlayPermission,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            PermissionRow(
                title = stringResource(R.string.permission_accessibility),
                description = stringResource(R.string.permission_accessibility_desc),
                granted = permissions.accessibility,
                onRequest = onRequestAccessibility,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            PermissionRow(
                title = stringResource(R.string.permission_notification),
                description = stringResource(R.string.permission_notification_desc),
                granted = permissions.notifications,
                onRequest = onRequestRuntimePermissions,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            PermissionRow(
                title = stringResource(R.string.permission_battery),
                description = stringResource(R.string.permission_battery_desc),
                granted = permissions.ignoringBatteryOptimizations,
                onRequest = onRequestBatteryOptimization,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (granted) {
            Text(
                text = stringResource(R.string.permission_granted),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            TextButton(onClick = onRequest) {
                Text(stringResource(R.string.action_grant))
            }
        }
    }
}

@Composable
private fun AutoStartCard(onOpenAutoStartSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.autostart_tip),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onOpenAutoStartSettings) {
                Text(stringResource(R.string.action_autostart))
            }
        }
    }
}
