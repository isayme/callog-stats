package org.isayme.callstats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.isayme.callstats.settings.SettingsRepository
import org.isayme.callstats.ui.CallStatsApp
import org.isayme.callstats.ui.CallStatsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            CallStatsTheme {
                CallStatsApp(settingsRepository)
            }
        }
    }
}
