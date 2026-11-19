package io.github.throughthewind.echo.ui.map

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.MeshChip
import io.github.throughthewind.echo.core.designsystem.component.ageLabel
import io.github.throughthewind.echo.core.designsystem.component.nearbyCount
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.mesh.RangeClass
import io.github.throughthewind.echo.core.mesh.TransportCapabilities
import io.github.throughthewind.echo.core.mesh.Unavailable
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.common.SecondaryButton
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/** What stands between the user and a working mesh, with its fix (FR-3.5, FR-3.3a). */
enum class MeshProblem {
    /** Bluetooth is off. Fix: Android's turn-on dialog. */
    RADIO_OFF,

    /** Nearby devices was refused. Fix: our sheet once, then Echo's page in Settings. */
    PERMISSION,

    /** The phone has no usable Bluetooth LE. No fix; the sheet says so. */
    UNSUPPORTED,

    /** The radio failed to start for a reason it did not name. */
    FAILED,

    /** The mesh was meant to be on and relaying stopped without the user (FR-3.3a). */
    STOPPED,
}

/** The problem [this] state shows, if any. [meantOn]: the user turned the mesh on and not off. */
fun MeshState.problem(meantOn: Boolean): MeshProblem? =
    when (this) {
        MeshState.Off -> {
            if (meantOn) MeshProblem.STOPPED else null
        }

        MeshState.Failed -> {
            MeshProblem.FAILED
        }

        is MeshState.Unavailable -> {
            when (reason) {
                Unavailable.RADIO_OFF -> MeshProblem.RADIO_OFF
                Unavailable.PERMISSION_MISSING -> MeshProblem.PERMISSION
                Unavailable.UNSUPPORTED -> MeshProblem.UNSUPPORTED
            }
        }

        MeshState.NoRadio, is MeshState.On -> {
            null
        }
    }

/**
 * The chip's state at [now] (components/MeshStatus): searching for 30 s, then alone, until someone is
 * heard. A problem reads "… · Fix".
 */
@Composable
fun MeshState.chip(
    now: Instant,
    meantOn: Boolean = false,
): MeshChip =
    when (this) {
        MeshState.Off -> {
            if (meantOn) MeshChip.Stopped else MeshChip.Off
        }

        is MeshState.Unavailable -> {
            MeshChip.NeedsFix(stringResource(problemLabel(problem(meantOn)!!)))
        }

        MeshState.NoRadio -> {
            MeshChip.NoRadio
        }

        MeshState.Failed -> {
            MeshChip.NeedsFix(stringResource(R.string.mesh_failed))
        }

        is MeshState.On -> {
            val age = lastReport?.let { now - it }
            when {
                nearby > 0 -> MeshChip.On(nearby, age)
                now - since < MeshNode.SEARCHING -> MeshChip.Searching
                else -> MeshChip.Alone(age)
            }
        }
    }

@StringRes
private fun problemLabel(problem: MeshProblem): Int =
    when (problem) {
        MeshProblem.RADIO_OFF -> R.string.mesh_radio_off
        MeshProblem.PERMISSION -> R.string.mesh_permission_needed
        MeshProblem.UNSUPPORTED -> R.string.mesh_unsupported
        MeshProblem.FAILED -> R.string.mesh_failed
        MeshProblem.STOPPED -> DS.string.ds_mesh_stopped
    }

/**
 * The mesh sheet (components/MeshStatus, patterns/MeshOnOff): the on/off switch, approximate facts,
 * the radios by what they can do (never by name), and the honesty callout. No peer is ever listed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshSheet(
    state: MeshState,
    radios: List<TransportCapabilities>,
    simulatedRadio: Boolean,
    now: Instant,
    meantOn: Boolean,
    onToggle: (Boolean) -> Unit,
    onFix: (MeshProblem) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                stringResource(R.string.mesh_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            state.problem(meantOn)?.let { problem -> Problem(problem, onFix) }
            if (state == MeshState.NoRadio) {
                Text(stringResource(R.string.mesh_no_radio), style = MaterialTheme.typography.bodyLarge)
            } else {
                val on = state is MeshState.On
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Touch.comfort)
                        .toggleable(value = on, role = Role.Switch, onValueChange = onToggle),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(if (on) R.string.mesh_switch_on else R.string.mesh_switch_off),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.mesh_switch_support),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = on, onCheckedChange = null)
                }
                if (state is MeshState.On) {
                    Fact(stringResource(R.string.mesh_phones_in_range), nearbyCount(state.nearby))
                    Fact(
                        stringResource(R.string.mesh_last_report),
                        state.lastReport?.let { stringResource(R.string.mesh_ago, ageLabel(now - it)) }
                            ?: stringResource(R.string.mesh_last_report_none),
                    )
                }
            }
            if (radios.isNotEmpty()) {
                Text(stringResource(R.string.mesh_radios), style = MaterialTheme.typography.titleMedium)
                radios.forEach { radio ->
                    val range =
                        when (radio.range) {
                            RangeClass.ROOM -> R.string.mesh_radio_short
                            RangeClass.STREET -> R.string.mesh_radio_street
                            RangeClass.BLOCK -> R.string.mesh_radio_block
                        }
                    val availability = if (radio.worksInBackground) R.string.mesh_radio_background else R.string.mesh_radio_foreground
                    Fact(stringResource(range), stringResource(availability))
                }
                if (simulatedRadio) {
                    Text(stringResource(R.string.mesh_radio_simulated), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                IconLine(
                    DS.drawable.ic_info,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    stringResource(R.string.mesh_honesty),
                    Modifier.padding(spacing.lg),
                )
            }
        }
    }
}

/** One sentence on what went wrong, and the fix as a button when there is one. */
@Composable
private fun Problem(
    problem: MeshProblem,
    onFix: (MeshProblem) -> Unit,
) {
    val (body, action) =
        when (problem) {
            MeshProblem.RADIO_OFF -> R.string.mesh_radio_off_body to R.string.mesh_radio_off_fix
            MeshProblem.PERMISSION -> R.string.mesh_permission_body to R.string.mesh_permission_fix
            MeshProblem.UNSUPPORTED -> R.string.mesh_unsupported_body to null
            MeshProblem.FAILED -> R.string.mesh_failed_body to null
            MeshProblem.STOPPED -> R.string.mesh_stopped_body to R.string.mesh_stopped_fix
        }
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(LocalSpacing.current.lg), verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
            IconLine(DS.drawable.ic_warning, MaterialTheme.colorScheme.onErrorContainer, stringResource(body))
            if (action != null) SecondaryButton(stringResource(action), { onFix(problem) })
        }
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
