package io.github.throughthewind.echo.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.mesh.DiagnosticsProvider
import io.github.throughthewind.echo.ui.common.BackBar
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import kotlinx.coroutines.launch

/**
 * Field diagnostics (F17, WI-036): the switch, what it counts and never counts, and, while on, the
 * whole export file in plain text before it can be shared (FR-17.3). What is shared is exactly the
 * text on screen; Refresh takes a new copy. Nothing is ever sent by itself.
 */
@Composable
fun DiagnosticsScreen(
    app: App,
    onBack: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val on by app.mesh.diagnosticsOn.collectAsStateWithLifecycle()
    var file by remember { mutableStateOf(app.mesh.diagnosticsExport()) }
    Scaffold(topBar = { BackBar(stringResource(R.string.diagnostics_title), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.comfort)
                    .toggleable(
                        value = on,
                        role = Role.Switch,
                        onValueChange = { next ->
                            scope.launch {
                                app.setDiagnostics(next)
                                file = app.mesh.diagnosticsExport()
                            }
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.diagnostics_switch),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = on, onCheckedChange = null)
            }
            Text(stringResource(R.string.diagnostics_counts), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.diagnostics_never), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.diagnostics_kept), style = MaterialTheme.typography.bodyLarge)
            val shown = file
            if (on && shown != null) {
                Text(
                    stringResource(R.string.diagnostics_review),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = spacing.lg).semantics { heading() },
                )
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                    Text(
                        shown,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().padding(spacing.lg),
                    )
                }
                PrimaryButton(
                    stringResource(R.string.diagnostics_share),
                    { context.startActivity(DiagnosticsProvider.share(context, shown)) },
                )
                SecondaryButton(stringResource(R.string.diagnostics_refresh), { file = app.mesh.diagnosticsExport() })
            }
        }
    }
}
