package io.github.throughthewind.echo.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.PinFamily
import io.github.throughthewind.echo.core.designsystem.theme.LocalGlanceType
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.mesh.group.GroupInfo
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.report.ReportAuthor
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.ReportType
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import io.github.throughthewind.echo.ui.family
import io.github.throughthewind.echo.ui.icon
import io.github.throughthewind.echo.ui.label
import io.github.throughthewind.echo.ui.pickerOrder
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

private const val COLUMNS = 3

/**
 * Drop a report (patterns/ReportPicker): a 3 × 3 grid of types, then one hero button naming the
 * audience. Not a modal sheet: the map above stays pannable, and the report goes at the crosshair,
 * never at the user's position (FR-4.2). There is no text field anywhere (FR-4.4). With groups, the
 * audience is chosen here: everyone nearby, unverified, or one group, signed and encrypted (FR-4.3).
 */
@Composable
fun ReportPicker(
    chosen: ReportType?,
    nextAllowed: (ReportType) -> Instant?,
    now: Instant,
    onChoose: (ReportType?) -> Unit,
    onSend: (ReportType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    groups: List<GroupInfo> = emptyList(),
    audience: Bytes? = null,
    onAudience: (Bytes?) -> Unit = {},
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = spacing.xs,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (chosen == null) stringResource(R.string.picker_title) else stringResource(chosen.label),
                    style = LocalGlanceType.current.title,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = onClose, modifier = Modifier.size(Touch.min)) {
                    Icon(painterResource(DS.drawable.ic_close), contentDescription = stringResource(R.string.close))
                }
            }
            if (chosen == null) {
                pickerOrder.chunked(COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        row.forEach { type ->
                            val blockedUntil = nextAllowed(type)
                            Tile(
                                type,
                                blockedUntil?.let { (now - (it - ReportAuthor.RATE_LIMIT)).inWholeSeconds.toInt() },
                                { onChoose(type) },
                                Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.picker_lasts, chosen.defaultLifetime.inWholeMinutes.toInt()),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (groups.isNotEmpty()) {
                    Text(
                        stringResource(R.string.picker_audience),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Column(Modifier.selectableGroup()) {
                        AudienceRow(stringResource(R.string.picker_audience_everyone), audience == null) { onAudience(null) }
                        groups.forEach { group ->
                            AudienceRow(stringResource(R.string.picker_audience_group, group.name), audience == group.id) {
                                onAudience(group.id)
                            }
                        }
                    }
                }
                val group = groups.firstOrNull { it.id == audience }
                Text(
                    if (group ==
                        null
                    ) {
                        stringResource(R.string.picker_unverified)
                    } else {
                        stringResource(R.string.picker_group_note, group.name)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    if (group ==
                        null
                    ) {
                        stringResource(R.string.picker_send_everyone)
                    } else {
                        stringResource(R.string.picker_send_group, group.name)
                    },
                    onClick = { onSend(chosen) },
                    modifier = Modifier.heightIn(min = Touch.hero),
                )
                SecondaryButton(stringResource(R.string.picker_change_type), onClick = { onChoose(null) })
            }
        }
    }
}

/** One audience, as a radio row at the minimum touch size. */
@Composable
private fun AudienceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.min)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A type tile, named by its type for TalkBack; disabled for a minute after sending that type (FR-4.6). */
@Composable
private fun Tile(
    type: ReportType,
    sentSecondsAgo: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val street = LocalStreetColors.current
    val (container, content) =
        when (type.family) {
            PinFamily.Hazard -> street.hazardContainer to street.onHazardContainer
            PinFamily.Blocked -> street.blockedContainer to street.onBlockedContainer
            PinFamily.Safe -> street.safeContainer to street.onSafeContainer
        }
    val name = stringResource(type.label)
    val limited = sentSecondsAgo != null
    val sentAgo = sentSecondsAgo?.let { stringResource(R.string.picker_sent_ago, it) }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
        modifier =
            modifier
                .heightIn(min = Touch.comfort)
                .alpha(if (limited) DISABLED_ALPHA else 1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(name, sentAgo).joinToString(", ")
                    if (limited) disabled()
                }.clickable(enabled = !limited, role = Role.Button, onClick = onClick),
    ) {
        Column(
            Modifier.padding(LocalSpacing.current.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
        ) {
            Icon(painterResource(type.icon), contentDescription = null, modifier = Modifier.size(28.dp))
            Text(
                sentAgo ?: name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.5f

/**
 * The grace period (FR-4.7, token `send-grace`): "Tear gas sending in 4 s · Cancel". A report that
 * has left the phone can only be withdrawn, so the copy says "sending". In discreet mode the
 * countdown does not tick.
 */
@Composable
fun SendingBar(
    type: ReportType,
    secondsLeft: Int,
    discreet: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (discreet) {
                    stringResource(R.string.sending_in_discreet, SEND_GRACE_SECONDS)
                } else {
                    stringResource(R.string.sending_in, stringResource(type.label), secondsLeft)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(width = Touch.comfort + spacing.lg, height = Touch.min)
                    .clickable(role = Role.Button, onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        }
    }
}

private val SEND_GRACE_SECONDS = MeshNode.SEND_GRACE.inWholeSeconds.toInt()
