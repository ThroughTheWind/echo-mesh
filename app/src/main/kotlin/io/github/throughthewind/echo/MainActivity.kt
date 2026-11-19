package io.github.throughthewind.echo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.core.designsystem.theme.DisplayPrefs
import io.github.throughthewind.echo.core.designsystem.theme.StreetTheme
import io.github.throughthewind.echo.ui.EchoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            StreetTheme(prefs = DisplayPrefs(discreet = settings.discreet)) {
                EchoApp(app)
            }
        }
    }
}
