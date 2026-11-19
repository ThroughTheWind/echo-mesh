package io.github.throughthewind.echo.ui.wipe

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.HoldToConfirmButton
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Opens the panic wipe sheet from anywhere (FR-11.1): the map's top bar and every screen's back bar
 * carry the button, so the wipe is at most two actions away. Null where there is nothing to wipe yet.
 */
val LocalPanicWipe = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * The panic wipe (patterns/PanicWipeConfirm): tap, then hold for 2 seconds. No second "are you
 * sure": the hold is the confirmation. At 2 s the key is destroyed first, then everything else, and
 * the app opens on its first-launch screen with no "wiped" message (FR-11.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicWipeSheet(
    app: App,
    onDismiss: () -> Unit,
) {
    val activity = LocalActivity.current
    val spacing = LocalSpacing.current
    var includeMaps by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                stringResource(R.string.wipe_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(if (includeMaps) R.string.wipe_body_maps else R.string.wipe_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(stringResource(R.string.wipe_others), style = MaterialTheme.typography.bodyLarge)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.min)
                    .toggleable(value = includeMaps, role = Role.Checkbox, onValueChange = { includeMaps = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Checkbox(checked = includeMaps, onCheckedChange = null)
                Text(stringResource(R.string.wipe_include_maps), style = MaterialTheme.typography.bodyLarge)
            }
            val holding = stringResource(R.string.wipe_keep_holding)
            HoldToConfirmButton(
                label = stringResource(R.string.wipe_hold),
                holdingLabel = { left -> holding.format(left) },
                accessibilityAction = stringResource(R.string.wipe_button),
                onConfirmed = {
                    // On the app's own scope, which the sheet closing cannot cancel: it must finish before the restart.
                    app.scope.launch {
                        app.wipe(includeMaps)
                        withContext(Dispatchers.Main) { if (activity != null) app.restartFresh(activity) }
                    }
                },
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = Touch.min)) {
                Text(stringResource(R.string.wipe_cancel), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
