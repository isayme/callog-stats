package org.isayme.callstats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.isayme.callstats.R
import org.isayme.callstats.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onDuplicateWindowChange: (Int) -> Unit,
    onLookbackMonthsChange: (Int) -> Unit,
    onNormalizeNumbersChange: (Boolean) -> Unit,
    onShowOnOutgoingChange: (Boolean) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    CounterRow(
                        title = stringResource(R.string.settings_duplicate_window),
                        description = stringResource(R.string.settings_duplicate_window_desc),
                        value = stringResource(
                            R.string.duration_minutes,
                            settings.duplicateWindowMinutes,
                        ),
                        canDecrease = settings.duplicateWindowMinutes > AppSettings.DUPLICATE_WINDOW_RANGE.first,
                        canIncrease = settings.duplicateWindowMinutes < AppSettings.DUPLICATE_WINDOW_RANGE.last,
                        onDecrease = {
                            onDuplicateWindowChange(settings.duplicateWindowMinutes - 1)
                        },
                        onIncrease = {
                            onDuplicateWindowChange(settings.duplicateWindowMinutes + 1)
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    CounterRow(
                        title = stringResource(R.string.settings_lookback),
                        description = stringResource(R.string.settings_lookback_desc),
                        value = stringResource(
                            R.string.duration_months,
                            settings.lookbackMonths,
                        ),
                        canDecrease = settings.lookbackMonths > AppSettings.LOOKBACK_MONTHS_RANGE.first,
                        canIncrease = settings.lookbackMonths < AppSettings.LOOKBACK_MONTHS_RANGE.last,
                        onDecrease = {
                            onLookbackMonthsChange(settings.lookbackMonths - 1)
                        },
                        onIncrease = {
                            onLookbackMonthsChange(settings.lookbackMonths + 1)
                        },
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.settings_normalize),
                        description = stringResource(R.string.settings_normalize_desc),
                        checked = settings.normalizeNumbers,
                        onCheckedChange = onNormalizeNumbersChange,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    SwitchRow(
                        title = stringResource(R.string.settings_show_outgoing),
                        description = stringResource(R.string.settings_show_outgoing_desc),
                        checked = settings.showOnOutgoing,
                        onCheckedChange = onShowOnOutgoingChange,
                    )
                }
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
private fun CounterRow(
    title: String,
    description: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
        OutlinedButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("−")
        }
        Text(
            text = value,
            modifier = Modifier.width(78.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        OutlinedButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("+")
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
