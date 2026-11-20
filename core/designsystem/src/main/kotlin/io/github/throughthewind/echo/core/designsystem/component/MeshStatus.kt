package io.github.throughthewind.echo.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.core.designsystem.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** What the mesh status chip shows (components/MeshStatus). Counts only, never who (FR-3.2). */
@Immutable
sealed interface MeshChip {
    data object Off : MeshChip

    /** The first 30 s after turning on, before anyone is heard. */
    data object Searching : MeshChip

    /**
     * [nearby] phones in range; [lastReport] is the age of the last report received, if any.
     * [lowDensity]: too few phones heard lately to trust an empty map (FR-16.1).
     */
    data class On(
        val nearby: Int,
        val lastReport: Duration?,
        val lowDensity: Boolean = false,
    ) : MeshChip

    /** Nobody heard for a minute. */
    data class Alone(
        val lastReport: Duration?,
    ) : MeshChip

    /** Something the user can fix, named in the user's terms ("Bluetooth off", "Permission needed"). */
    data class NeedsFix(
        val label: String,
    ) : MeshChip

    /** The mesh was meant to be on and relaying stopped (FR-3.3a). */
    data object Stopped : MeshChip

    /** This build has no radio at all: a release before the first transport. */
    data object NoRadio : MeshChip
}

/** Counts cap at 20+: precision past that is false. */
fun nearbyCount(count: Int): String = if (count > NEARBY_CAP) "$NEARBY_CAP+" else count.toString()

private const val NEARBY_CAP = 20

/** "8s", "4 min", "2 h". */
@Composable
fun ageLabel(age: Duration): String =
    when {
        age < 1.minutes -> stringResource(R.string.ds_age_seconds, age.inWholeSeconds.toInt().coerceAtLeast(0))
        age < 1.hours -> stringResource(R.string.ds_age_minutes, age.inWholeMinutes.toInt())
        else -> stringResource(R.string.ds_age_hours, age.inWholeHours.toInt())
    }

/**
 * The mesh's always-visible status and its on/off control: never hidden, discreet mode included.
 * 48dp tall inside a [Touch.min] hit area; the content description reads the whole state.
 */
@Composable
fun MeshStatusChip(
    state: MeshChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val street = LocalStreetColors.current
    val (background, content) =
        when (state) {
            is MeshChip.On -> {
                if (state.lowDensity) {
                    street.blockedContainer to street.onBlockedContainer
                } else {
                    scheme.surfaceContainerHigh to
                        scheme.onSurface
                }
            }

            MeshChip.Searching -> {
                scheme.surfaceContainerHigh to scheme.onSurface
            }

            is MeshChip.Alone -> {
                street.blockedContainer to street.onBlockedContainer
            }

            MeshChip.Off, MeshChip.NoRadio -> {
                scheme.surfaceContainerHighest to scheme.onSurface
            }

            is MeshChip.NeedsFix, MeshChip.Stopped -> {
                scheme.errorContainer to scheme.onErrorContainer
            }
        }
    val icon =
        when (state) {
            is MeshChip.On -> if (state.lowDensity) R.drawable.ic_warning else R.drawable.ic_radar
            MeshChip.Searching -> R.drawable.ic_radar
            is MeshChip.Alone, is MeshChip.NeedsFix, MeshChip.Stopped -> R.drawable.ic_warning
            MeshChip.Off, MeshChip.NoRadio -> R.drawable.ic_sensors_off
        }
    val iconTint = if (state is MeshChip.On && !state.lowDensity) scheme.primary else content
    val (label, action) = chipText(state)
    val description = chipDescription(state, label, action)
    Box(
        modifier
            .defaultMinSize(minHeight = Touch.min)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = background,
            contentColor = content,
            modifier = Modifier.height(48.dp),
        ) {
            Row(
                Modifier.padding(horizontal = LocalSpacing.current.lg),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal))
                if (action != null) {
                    Text("·", style = MaterialTheme.typography.labelLarge, color = Color.Unspecified)
                    Text(action, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun chipText(state: MeshChip): Pair<String, String?> =
    when (state) {
        MeshChip.Off -> {
            stringResource(R.string.ds_mesh_off) to stringResource(R.string.ds_mesh_turn_on)
        }

        MeshChip.Searching -> {
            stringResource(R.string.ds_mesh_searching) to null
        }

        is MeshChip.On -> {
            val label = if (state.lowDensity) R.string.ds_mesh_few else R.string.ds_mesh_nearby
            stringResource(label, nearbyCount(state.nearby)) to state.lastReport?.let { ageLabel(it) }
        }

        is MeshChip.Alone -> {
            stringResource(R.string.ds_mesh_alone) to state.lastReport?.let { ageLabel(it) }
        }

        is MeshChip.NeedsFix -> {
            state.label to stringResource(R.string.ds_mesh_fix)
        }

        MeshChip.Stopped -> {
            stringResource(R.string.ds_mesh_stopped) to stringResource(R.string.ds_mesh_fix)
        }

        MeshChip.NoRadio -> {
            stringResource(R.string.ds_mesh_no_radio) to null
        }
    }

@Composable
private fun chipDescription(
    state: MeshChip,
    label: String,
    action: String?,
): String =
    when (state) {
        is MeshChip.On -> {
            val count = nearbyCount(state.nearby)
            val said =
                state.lastReport?.let { pluralStringResource(R.plurals.ds_mesh_described_on, state.nearby, count, ageLabel(it)) }
                    ?: pluralStringResource(R.plurals.ds_mesh_described_on_none, state.nearby, count)
            if (state.lowDensity) "$said. ${stringResource(R.string.ds_mesh_described_few)}" else said
        }

        else -> {
            listOfNotNull(label, action).joinToString(", ")
        }
    }
