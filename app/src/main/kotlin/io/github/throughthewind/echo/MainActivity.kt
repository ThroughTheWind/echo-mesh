package io.github.throughthewind.echo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.core.data.Settings
import io.github.throughthewind.echo.core.designsystem.theme.DisplayPrefs
import io.github.throughthewind.echo.core.designsystem.theme.HapticsLevel
import io.github.throughthewind.echo.core.designsystem.theme.StreetTheme
import io.github.throughthewind.echo.core.designsystem.theme.ThemePref
import io.github.throughthewind.echo.ui.EchoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            StreetTheme(prefs = settings.displayPrefs()) {
                EchoApp(app)
            }
        }
    }
}

/** Settings > Display and Feedback as the design system reads them; an unknown name falls back to the default. */
fun Settings.displayPrefs(): DisplayPrefs =
    DisplayPrefs(
        theme = ThemePref.entries.firstOrNull { it.name == theme } ?: ThemePref.Auto,
        discreet = discreet,
        largePins = largePins,
        haptics = HapticsLevel.entries.firstOrNull { it.name == haptics } ?: HapticsLevel.Light,
    )
