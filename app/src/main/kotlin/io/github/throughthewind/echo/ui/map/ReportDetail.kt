package io.github.throughthewind.echo.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.ExpiryBar
import io.github.throughthewind.echo.core.designsystem.component.ReportPin
import io.github.throughthewind.echo.core.designsystem.component.dashedBorder
import io.github.throughthewind.echo.core.designsystem.component.groupColor
import io.github.throughthewind.echo.core.designsystem.theme.LocalGlanceType
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.ui.LocalGroups
import io.github.throughthewind.echo.ui.colourOf
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.describe
import io.github.throughthewind.echo.ui.distanceFrom
import io.github.throughthewind.echo.ui.label
import io.github.throughthewind.echo.ui.look
import io.github.throughthewind.echo.ui.reportedAgo
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/**
 * What a report is, where, how long it lasts and who vouches for it (patterns/ReportDetail). Public
 * reports name no sender, ever. Still there / Gone votes come with WI-019; withdrawing your own
 * report is here (FR-4.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailSheet(
    shown: ShownReport,
    from: GeoPoint?,
    now: Instant,
    onWithdraw: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val report = shown.report
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                ReportPin(shown.look(now), contentDescription = shown.describe(now), onClick = {})
                Column {
                    Text(
                        stringResource(report.type.label),
                        style = LocalGlanceType.current.title,
                        modifier = Modifier.semantics { heading() },
                    )
                    val where = from?.let { distanceFrom(it, report.position) }
                    Text(
                        listOfNotNull(where, reportedAgo(report.createdAt, now)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ExpiryBar(left = report.expiresAt - now, lifetime = report.lifetime)
            Provenance(shown)
            if (shown.mine) {
                FilledTonalButton(
                    onClick = onWithdraw,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch.comfort),
                ) {
                    Text(stringResource(R.string.detail_withdraw), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    stringResource(R.string.detail_votes_later),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The honest answer to "should I trust this?": a dashed box for anyone-nearby reports. */
@Composable
private fun Provenance(shown: ShownReport) {
    val title: String
    val body: String
    val tag = shown.group
    val travel = if (shown.hops == 0) stringResource(R.string.detail_direct) else stringResource(R.string.detail_relayed, shown.hops)
    if (tag != null) {
        // A group report names its member (threat model §7), which is what makes muting possible.
        val text =
            if (shown.mine) {
                stringResource(R.string.detail_yours_title) + " " + stringResource(R.string.detail_yours_group_body, tag.groupName)
            } else {
                stringResource(R.string.detail_group_title, tag.sender, tag.groupName) + " " +
                    stringResource(R.string.detail_group_body, tag.sender) + " " + travel
            }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(groupColor(LocalStreetColors.current, LocalGroups.current.colourOf(tag.groupId))),
                )
                Box(Modifier.padding(LocalSpacing.current.lg)) {
                    IconLine(DS.drawable.ic_lock, MaterialTheme.colorScheme.onSurfaceVariant, text)
                }
            }
        }
        return
    }
    if (shown.mine) {
        title = stringResource(R.string.detail_yours_title)
        body = stringResource(R.string.detail_yours_body)
    } else {
        title = stringResource(R.string.detail_public_title)
        body = stringResource(R.string.detail_public_body) + " " + travel
    }
    Column(
        Modifier
            .fillMaxWidth()
            .dashedBorder(MaterialTheme.colorScheme.outline)
            .padding(LocalSpacing.current.lg),
    ) {
        IconLine(DS.drawable.ic_info, MaterialTheme.colorScheme.onSurfaceVariant, "$title $body")
    }
}
