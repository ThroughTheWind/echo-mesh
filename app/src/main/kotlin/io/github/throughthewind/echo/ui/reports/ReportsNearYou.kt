package io.github.throughthewind.echo.ui.reports

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.ReportPin
import io.github.throughthewind.echo.core.designsystem.component.timeLeftCompact
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.map.OwnPosition
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.mesh.node.couldBeATrap
import io.github.throughthewind.echo.core.model.Geo
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.ui.LocalGroups
import io.github.throughthewind.echo.ui.MapsState
import io.github.throughthewind.echo.ui.common.BackBar
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.describe
import io.github.throughthewind.echo.ui.distanceFrom
import io.github.throughthewind.echo.ui.label
import io.github.throughthewind.echo.ui.look
import io.github.throughthewind.echo.ui.map.ReportDetailSheet
import io.github.throughthewind.echo.ui.rememberNow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/**
 * Every report the phone holds, nearest first (screens/ReportsNearYou, FR-5.6): the way in for
 * TalkBack and for anyone who would rather read than scan a map. Same pins, same detail as the map.
 * The same Everything / My groups filter as the map (FR-9.3).
 */
@Composable
fun ReportsNearYou(
    app: App,
    maps: MapsState,
    onBack: () -> Unit,
    onReportHere: () -> Unit,
    groupsOnly: Boolean,
    onGroupsOnly: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by app.mesh.reports.collectAsStateWithLifecycle()
    val now = rememberNow()
    val located =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    val fix by remember(located) { if (located) OwnPosition.updates(context) else emptyFlow() }.collectAsStateWithLifecycle(null)
    val from: GeoPoint? = fix?.let { GeoPoint(it.latitude, it.longitude) } ?: maps.lastCentre
    var detailFor by remember { mutableStateOf<ShownReport?>(null) }

    // Live reports, nearest first. Order is fixed per report set, so rows don't jump under TalkBack
    // focus as the position moves a little.
    val rows =
        remember(reports, from == null) {
            reports.sortedBy { shown -> from?.let { Geo.distanceMeters(it, shown.report.position) } ?: 0.0 }
        }.filter { now < it.report.expiresAt && (!groupsOnly || it.group != null) }
    val groups = LocalGroups.current

    Scaffold(topBar = { BackBar(stringResource(R.string.reports_title), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
        ) {
            if (groups.isNotEmpty()) {
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalSpacing.current.lg, vertical = LocalSpacing.current.sm),
                ) {
                    listOf(false to R.string.filter_everything, true to R.string.filter_my_groups).forEachIndexed { i, (only, label) ->
                        SegmentedButton(
                            selected = groupsOnly == only,
                            onClick = { onGroupsOnly(only) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            if (fix == null && from != null) {
                Text(
                    stringResource(R.string.reports_from_centre),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = LocalSpacing.current.lg),
                )
            }
            if (rows.isEmpty()) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(LocalSpacing.current.xxl),
                    verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
                ) {
                    Text(stringResource(R.string.reports_empty_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.reports_empty_body), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(rows) { shown ->
                        ReportRow(shown, from, now) { detailFor = shown }
                    }
                }
            }
            if (maps.current != null) {
                PrimaryButton(
                    stringResource(R.string.reports_report_here),
                    onClick = onReportHere,
                    modifier = Modifier.padding(LocalSpacing.current.lg),
                )
            }
        }
    }

    detailFor?.let { shown ->
        val live = reports.firstOrNull { it.id == shown.id }
        if (live == null) {
            detailFor = null
        } else {
            ReportDetailSheet(
                shown = live,
                from = from,
                now = now,
                onWithdraw = {
                    scope.launch {
                        app.mesh.withdraw(live.id)
                        detailFor = null
                    }
                },
                onVote = { vote -> scope.launch { app.mesh.vote(live.id, vote) } },
                onDismiss = { detailFor = null },
            )
        }
    }
}

/** Pin, type, then "distance direction · source"; compact time left at the end. */
@Composable
private fun ReportRow(
    shown: ShownReport,
    from: GeoPoint?,
    now: Instant,
    onClick: () -> Unit,
) {
    val tag = shown.group
    val source =
        when {
            shown.mine -> stringResource(R.string.row_source_yours)
            tag != null -> stringResource(R.string.row_source_group, tag.sender, tag.groupName)
            else -> stringResource(R.string.row_source_nearby)
        }
    val where = from?.let { distanceFrom(it, shown.report.position) }
    ListItem(
        leadingContent = { ReportPin(shown.look(now), contentDescription = shown.describe(now), onClick = onClick) },
        headlineContent = { Text(stringResource(shown.report.type.label), style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Column {
                Text(listOfNotNull(where, source).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                if (shown.couldBeATrap) {
                    // FR-15.2: the same warning as the pin's "?", in words.
                    IconLine(DS.drawable.ic_warning, LocalStreetColors.current.blocked, stringResource(R.string.row_could_be_trap))
                }
            }
        },
        trailingContent = { Text(timeLeftCompact(shown.report.expiresAt - now), style = MaterialTheme.typography.labelLarge) },
        modifier =
            Modifier
                .heightIn(min = Touch.comfort)
                .clickable(role = Role.Button, onClick = onClick),
    )
}
