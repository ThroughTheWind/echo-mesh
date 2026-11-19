package io.github.throughthewind.echo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.throughthewind.echo.core.designsystem.theme.DisplayPrefs
import io.github.throughthewind.echo.core.designsystem.theme.StreetTheme
import io.github.throughthewind.echo.ui.EchoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            // Display preferences join the sealed settings with discreet mode (WI-025).
            StreetTheme(prefs = DisplayPrefs()) {
                EchoApp(app)
            }
        }
    }
}
