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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import io.github.throughthewind.echo.core.mesh.node.couldBeATrap
import io.github.throughthewind.echo.core.mesh.node.publicConfirmationsHidden
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.ui.LocalGroups
import io.github.throughthewind.echo.ui.colourOf
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.describe
import io.github.throughthewind.echo.ui.distanceFrom
import io.github.throughthewind.echo.ui.groups.Callout
import io.github.throughthewind.echo.ui.label
import io.github.throughthewind.echo.ui.look
import io.github.throughthewind.echo.ui.reportedAgo
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/**
 * What a report is, where, how long it lasts and who vouches for it (patterns/ReportDetail). Public
 * reports name no sender, ever. Still there / Gone, once per report (FR-6.1), with the votes split
 * into the group's and nearby phones' (FR-6.2), never called "verified" (FR-6.5); your own report
 * has Withdraw instead (FR-4.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailSheet(
    shown: ShownReport,
    from: GeoPoint?,
    now: Instant,
    onWithdraw: () -> Unit,
    onVote: (Vote) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val report = shown.report
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (shown.tally.probablyGone) PROBABLY_GONE_ALPHA else 1f),
            ) {
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
            if (shown.couldBeATrap) Callout(stringResource(R.string.detail_could_be_trap), warning = true)
            if (shown.tally.probablyGone) ProbablyGone(shown)
            Provenance(shown)
            VotesLine(shown)
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
                VoteButtons(shown.myVote, onVote)
            }
        }
    }
}

/** The header's fade when a report is probably gone (token `probably-gone`, components/ReportPin). */
private const val PROBABLY_GONE_ALPHA = 0.5f

/** Which rule faded the report (FR-6.4): a group member's dismissal, or far more public ones. */
@Composable
private fun ProbablyGone(shown: ShownReport) {
    val groups = LocalGroups.current
    val group = shown.group?.groupName ?: groups.singleOrNull()?.name ?: stringResource(R.string.votes_your_groups)
    Callout(
        if (shown.tally.fadedByGroup) {
            stringResource(R.string.detail_gone_group, group)
        } else {
            stringResource(R.string.detail_gone_public)
        },
    )
}

/** "Crew: 2 confirmed · Nearby: 3 confirmed, 1 said gone" (FR-6.2), counts and nothing more. */
@Composable
private fun VotesLine(shown: ShownReport) {
    val t = shown.tally
    val groups = LocalGroups.current
    val group = shown.group?.groupName ?: groups.singleOrNull()?.name ?: stringResource(R.string.votes_your_groups)
    val parts =
        listOfNotNull(
            counts(t.groupConfirmed, t.groupDismissed)?.let { stringResource(R.string.votes_from, group, it) },
            // FR-15.3: no public confirmation count on a public steering report; dismissals still show.
            counts(
                if (shown.publicConfirmationsHidden) 0 else t.nearbyConfirmed,
                t.nearbyDismissed,
            )?.let { stringResource(R.string.votes_from, stringResource(R.string.votes_nearby), it) },
        )
    Text(
        if (parts.isEmpty()) stringResource(R.string.votes_none) else parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "3 confirmed, 1 said gone", or null when nobody voted. */
@Composable
private fun counts(
    confirmed: Int,
    dismissed: Int,
): String? =
    listOfNotNull(
        if (confirmed > 0) stringResource(R.string.votes_confirmed, confirmed) else null,
        if (dismissed > 0) stringResource(R.string.votes_said_gone, dismissed) else null,
    ).joinToString(", ").ifEmpty { null }

/** Still there (tonal) and Gone (outlined), side by side; one vote per report, so both lock after it. */
@Composable
private fun VoteButtons(
    mine: Vote?,
    onVote: (Vote) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
        val tall = Modifier.weight(1f).heightIn(min = Touch.comfort)
        FilledTonalButton(onClick = { onVote(Vote.CONFIRM) }, enabled = mine == null, modifier = tall) {
            Text(
                stringResource(if (mine == Vote.CONFIRM) R.string.vote_said_there else R.string.vote_still_there),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        OutlinedButton(onClick = { onVote(Vote.DISMISS) }, enabled = mine == null, modifier = tall) {
            Text(
                stringResource(if (mine == Vote.DISMISS) R.string.vote_said_gone else R.string.vote_gone),
                style = MaterialTheme.typography.labelLarge,
            )
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
