package io.github.throughthewind.echo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.core.data.Settings
import io.github.throughthewind.echo.core.designsystem.theme.DisplayPrefs
import io.github.throughthewind.echo.core.designsystem.theme.HapticsLevel
import io.github.throughthewind.echo.core.designsystem.theme.StreetTheme
import io.github.throughthewind.echo.core.designsystem.theme.ThemePref
import io.github.throughthewind.echo.ui.EchoApp

private const val HALF = 0.5f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        val app = application as App
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            StreetTheme(prefs = settings.displayPrefs()) {
                // Echo picks its own theme (Sunlight is light even when the phone is dark), so the
                // system bars' icons follow Echo's background, not the phone's setting.
                val light = MaterialTheme.colorScheme.background.luminance() > HALF
                LaunchedEffect(light) {
                    val bars =
                        if (light) {
                            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        }
                    enableEdgeToEdge(bars, bars)
                }
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
