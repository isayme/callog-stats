package org.isayme.callstats.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppSettings(
                monitoringEnabled = preferences[Keys.MONITORING_ENABLED] ?: false,
                duplicateWindowMinutes = preferences[Keys.DUPLICATE_WINDOW_MINUTES]
                    ?: AppSettings.DEFAULT_DUPLICATE_WINDOW_MINUTES,
                lookbackMonths = preferences[Keys.LOOKBACK_MONTHS]
                    ?: AppSettings.DEFAULT_LOOKBACK_MONTHS,
                normalizeNumbers = preferences[Keys.NORMALIZE_NUMBERS] ?: true,
                showOnOutgoing = preferences[Keys.SHOW_ON_OUTGOING] ?: true,
            )
        }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    suspend fun setDuplicateWindowMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.DUPLICATE_WINDOW_MINUTES] = minutes }
    }

    suspend fun setLookbackMonths(months: Int) {
        context.settingsDataStore.edit { it[Keys.LOOKBACK_MONTHS] = months }
    }

    suspend fun setNormalizeNumbers(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NORMALIZE_NUMBERS] = enabled }
    }

    suspend fun setShowOnOutgoing(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_ON_OUTGOING] = enabled }
    }

    private object Keys {
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val DUPLICATE_WINDOW_MINUTES = intPreferencesKey("duplicate_window_minutes")
        val LOOKBACK_MONTHS = intPreferencesKey("lookback_months")
        val NORMALIZE_NUMBERS = booleanPreferencesKey("normalize_numbers")
        val SHOW_ON_OUTGOING = booleanPreferencesKey("show_on_outgoing")
    }
}
