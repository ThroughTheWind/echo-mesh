package io.github.throughthewind.echo.ui.map

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.MeshStatusChip
import io.github.throughthewind.echo.core.designsystem.component.ReportPin
import io.github.throughthewind.echo.core.designsystem.theme.HapticsLevel
import io.github.throughthewind.echo.core.designsystem.theme.LocalHaptics
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetMode
import io.github.throughthewind.echo.core.designsystem.theme.StreetMode
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.map.MapCamera
import io.github.throughthewind.echo.core.map.OfflineMap
import io.github.throughthewind.echo.core.map.OwnFix
import io.github.throughthewind.echo.core.map.OwnPosition
import io.github.throughthewind.echo.core.map.rememberMapCamera
import io.github.throughthewind.echo.core.mesh.group.MemberPosition
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import io.github.throughthewind.echo.ui.LocalGroups
import io.github.throughthewind.echo.ui.MapsState
import io.github.throughthewind.echo.ui.colourOf
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import io.github.throughthewind.echo.ui.describe
import io.github.throughthewind.echo.ui.family
import io.github.throughthewind.echo.ui.groups.MARKER_DP
import io.github.throughthewind.echo.ui.groups.MemberMarker
import io.github.throughthewind.echo.ui.groups.SharingBars
import io.github.throughthewind.echo.ui.groups.message
import io.github.throughthewind.echo.ui.hex
import io.github.throughthewind.echo.ui.look
import io.github.throughthewind.echo.ui.maps.GetMapChoices
import io.github.throughthewind.echo.ui.rememberNow
import io.github.throughthewind.echo.ui.wipe.LocalPanicWipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/**
 * The home screen (screens/MapView): the offline map, the reports this phone holds as pins, the
 * mesh status and its on/off control, and the Report button in the thumb zone.
 */
@Composable
fun MapScreen(
    app: App,
    maps: MapsState,
    openPicker: Boolean,
    onPickerOpened: () -> Unit,
    meshOnRequested: Boolean,
    onMeshOnHandled: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenGroups: () -> Unit,
    groupsOnly: Boolean,
    onGroupsOnly: (Boolean) -> Unit,
    focus: GeoPoint?,
    onFocused: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mesh = app.mesh
    val meshState by mesh.state.collectAsStateWithLifecycle()
    val reports by mesh.reports.collectAsStateWithLifecycle()
    val pending by mesh.pending.collectAsStateWithLifecycle()
    val sharing by mesh.sharing.collectAsStateWithLifecycle()
    val positions by mesh.positions.collectAsStateWithLifecycle()
    val sharingStopped by mesh.sharingStopped.collectAsStateWithLifecycle()
    val diagnosticsOn by mesh.diagnosticsOn.collectAsStateWithLifecycle()
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val now = rememberNow(if (pending != null) 250.milliseconds else 1000.milliseconds)
    val camera = rememberMapCamera()
    val entry = maps.current
    val snackbar = remember { SnackbarHostState() }

    var locationGranted by remember { mutableStateOf(hasLocation(context)) }
    var locationAsked by rememberSaveable { mutableStateOf(false) }
    var showLocationSheet by rememberSaveable { mutableStateOf(false) }
    // Granted in Android's Settings while Echo was in the background: pick it up on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { locationGranted = hasLocation(context) }
    val ownFix by remember(locationGranted) {
        if (locationGranted) OwnPosition.updates(context) else emptyFlow()
    }.collectAsStateWithLifecycle(null)

    var picking by rememberSaveable { mutableStateOf(false) }
    // Who a report goes to: null is everyone nearby. Defaults to the last choice (FR-4.3).
    val groups = LocalGroups.current
    var audience by remember { mutableStateOf<Bytes?>(null) }
    LaunchedEffect(picking) {
        if (picking) {
            audience = if (settings.lastAudienceIsPublic) null else groups.firstOrNull { it.id.hex() == settings.lastAudienceGroup }?.id
        }
    }
    var chosen by rememberSaveable { mutableStateOf<ReportType?>(null) }
    var detailFor by remember { mutableStateOf<ShownReport?>(null) }
    var showMeshSheet by rememberSaveable { mutableStateOf(false) }
    var showGetMap by rememberSaveable { mutableStateOf(false) }
    var showNearbySheet by rememberSaveable { mutableStateOf(false) }
    var showNotificationsSheet by rememberSaveable { mutableStateOf(false) }
    var pickerHeight by remember { mutableIntStateOf(0) }

    val sentMessage = stringResource(R.string.sent)
    val meshOnMessage = stringResource(R.string.mesh_on_snackbar)
    val meshOffMessage = stringResource(R.string.mesh_off_snackbar)
    val withdrawnMessage = stringResource(R.string.detail_withdrawn)
    val locationRefused = stringResource(R.string.location_refused)
    val locationSettings = stringResource(R.string.location_open_settings)
    val haptics = LocalHapticFeedback.current
    val hapticsLevel = LocalHaptics.current

    LaunchedEffect(camera) {
        snapshotFlow { camera.center() }.collect { centre -> if (centre != null) maps.lastCentre = centre }
    }

    LaunchedEffect(openPicker) {
        if (openPicker) {
            picking = true
            onPickerOpened()
        }
    }

    // A member's position picked on the roster: centre on it once.
    LaunchedEffect(focus) {
        if (focus != null) {
            camera.centreOn(focus)
            onFocused()
        }
    }

    // Someone joined one of this phone's groups through another member: everyone sees it (FR-8.2).
    val joinedFormat = stringResource(R.string.joined_snackbar)
    LaunchedEffect(mesh) {
        mesh.groupChannel?.joins?.collect { join ->
            if (!join.throughThisPhone) snackbar.showSnackbar(joinedFormat.format(join.nickname, join.groupName))
        }
    }

    // Sharing that stopped by itself says why, the next time the map is visible (FR-10.1).
    val stoppedMessage = sharingStopped?.message()
    LaunchedEffect(sharingStopped) {
        if (stoppedMessage != null) {
            // Its own scope: noting it changes this effect's key, which would cancel the snackbar.
            scope.launch { snackbar.showSnackbar(stoppedMessage) }
            mesh.sharingStopNoted()
        }
    }

    // A report that leaves its grace period without a Cancel was sent (FR-4.7).
    var cancelled by remember { mutableStateOf(false) }
    var wasPending by remember { mutableStateOf(false) }
    LaunchedEffect(pending) {
        if (pending != null) {
            wasPending = true
            cancelled = false
        } else if (wasPending) {
            wasPending = false
            if (!cancelled) {
                if (hapticsLevel != HapticsLevel.Off) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                snackbar.showSnackbar(sentMessage)
            }
        }
    }

    // Android may refuse without showing its dialog (a past refusal, or the phone's own policy), so a
    // refusal always offers Echo's page in Settings: never a dead end.
    fun showLocationRefused() {
        scope.launch {
            val result = snackbar.showSnackbar(locationRefused, actionLabel = locationSettings, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) openAppSettings(context)
        }
    }

    val locationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            locationGranted = result.values.any { it }
            if (!locationGranted) showLocationRefused()
        }

    fun centreOnMe() {
        val fix = ownFix
        when {
            fix != null -> camera.centreOn(fix.point())
            locationGranted -> Unit
            locationAsked -> showLocationRefused()
            else -> showLocationSheet = true
        }
    }

    fun switchMesh(on: Boolean) {
        scope.launch {
            if (on) {
                if (app.meshOn()) snackbar.showSnackbar(meshOnMessage)
            } else {
                app.meshOff()
                snackbar.showSnackbar(meshOffMessage)
            }
        }
    }

    // Turning on, first time only: our Nearby devices sheet, then our Notifications sheet, each
    // before Android's dialog (patterns/PermissionsInContext). A refusal still turns the mesh on,
    // so the chip shows the fix in context; the dialog is never shown twice.
    fun turnMeshOn() {
        val nearbyMissing = app.meshPermissions.any { !granted(context, it) }
        val notificationsMissing =
            !app.simulatedRadio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !granted(context, Manifest.permission.POST_NOTIFICATIONS)
        when {
            nearbyMissing && !settings.nearbyAsked -> showNearbySheet = true
            !nearbyMissing && notificationsMissing && !settings.notificationsAsked -> showNotificationsSheet = true
            else -> switchMesh(true)
        }
    }

    fun toggleMesh(on: Boolean) = if (on) turnMeshOn() else switchMesh(false)

    // The mesh switch in Settings: the same first-time sheets, shown here on the map.
    LaunchedEffect(meshOnRequested) {
        if (meshOnRequested) {
            onMeshOnHandled()
            turnMeshOn()
        }
    }

    val nearbyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { turnMeshOn() }
    val notificationsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { switchMesh(true) }
    val bluetoothLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { switchMesh(true) }

    fun fix(problem: MeshProblem) {
        when (problem) {
            MeshProblem.RADIO_OFF -> {
                runCatching { bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
            }

            MeshProblem.PERMISSION -> {
                if (settings.nearbyAsked) openAppSettings(context) else showNearbySheet = true
            }

            MeshProblem.STOPPED -> {
                runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }

            MeshProblem.UNSUPPORTED, MeshProblem.FAILED -> {
                Unit
            }
        }
    }

    val listLabel = stringResource(R.string.map_reports_near_you)
    val mapDescription = pluralStringResource(R.plurals.map_described, reports.size, reports.size)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = mapDescription
                customActions =
                    listOf(
                        CustomAccessibilityAction(listLabel) {
                            onOpenList()
                            true
                        },
                    )
            },
    ) {
        val density = LocalDensity.current
        if (entry != null) {
            OfflineMap(entry, ownFix, camera, Modifier.fillMaxSize())
            // My groups only hides public pins, your own public ones included (FR-9.3).
            Pins(if (groupsOnly) reports.filter { it.group != null } else reports, camera, now, onClick = { detailFor = it })
            Members(positions, camera, now)
        } else {
            NoMap(onGetMap = { showGetMap = true })
        }

        if (picking && entry != null) {
            val above = constraints.maxHeight - pickerHeight
            Crosshair(Modifier.offset { IntOffset(constraints.maxWidth / 2, above / 2) - crosshairHalf(density.density) })
        }

        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = LocalSpacing.current.lg)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
        ) {
            // Four 64 dp buttons leave a phone too little width for the chip beside them (its label
            // grows with the count and the language), so the buttons hold the top corner and the
            // chip sits under them; on one line the panic wipe was pushed off the screen.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md, Alignment.End),
            ) {
                MapIconButton(DS.drawable.ic_group, stringResource(R.string.groups_title), onOpenGroups)
                MapIconButton(DS.drawable.ic_settings, stringResource(R.string.settings_open), onOpenSettings)
                // Discreet mode: one tap, no confirm, the same button turns it off (FR-13.1).
                MapIconButton(
                    if (settings.discreet) DS.drawable.ic_visibility_off else DS.drawable.ic_visibility,
                    stringResource(if (settings.discreet) R.string.discreet_on else R.string.discreet_off),
                    { scope.launch { app.setDiscreet(!settings.discreet) } },
                )
                LocalPanicWipe.current?.let { wipe ->
                    MapIconButton(DS.drawable.ic_delete_forever, stringResource(R.string.wipe_button), wipe)
                }
            }
            MeshStatusChip(
                state = meshState.chip(now, settings.meshMeantOn),
                onClick = {
                    when (meshState.problem(settings.meshMeantOn)) {
                        null -> if (meshState == MeshState.Off) turnMeshOn() else showMeshSheet = true
                        MeshProblem.RADIO_OFF -> fix(MeshProblem.RADIO_OFF)
                        MeshProblem.PERMISSION -> fix(MeshProblem.PERMISSION)
                        else -> showMeshSheet = true
                    }
                },
            )
            // While sharing, one bar per group with its own Stop (FR-10.4).
            SharingBars(sharing, now, onStop = mesh::stopSharing)
            // FR-16.2: with nobody in range, the map says so instead of looking like a quiet street.
            val alone = meshState as? MeshState.On
            if (alone != null && alone.nearby == 0 && now - alone.since >= MeshNode.SEARCHING) {
                Surface(
                    color = LocalStreetColors.current.blockedContainer,
                    contentColor = LocalStreetColors.current.onBlockedContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(horizontal = LocalSpacing.current.lg),
                ) {
                    IconLine(
                        DS.drawable.ic_warning,
                        LocalStreetColors.current.onBlockedContainer,
                        stringResource(R.string.map_alone),
                        Modifier.padding(LocalSpacing.current.md),
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(LocalSpacing.current.lg),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
        ) {
            SnackbarHost(snackbar)
            pending?.let { report ->
                val seconds = ceil((report.sendsAt - now).inWholeMilliseconds / MILLIS_PER_SECOND).toInt().coerceAtLeast(0)
                SendingBar(
                    type = report.type,
                    secondsLeft = seconds,
                    discreet = LocalStreetMode.current == StreetMode.Discreet,
                    onCancel = {
                        cancelled = true
                        mesh.cancelPending()
                    },
                )
            }
            if (!picking) {
                Row(verticalAlignment = Alignment.Bottom) {
                    MapIconButton(DS.drawable.ic_near_me, listLabel, onOpenList)
                    if (groups.isNotEmpty()) {
                        Box(Modifier.size(LocalSpacing.current.md))
                        MapIconButton(
                            if (groupsOnly) DS.drawable.ic_group else DS.drawable.ic_filter_alt,
                            stringResource(if (groupsOnly) R.string.filter_show_everything else R.string.filter_groups_only),
                            { onGroupsOnly(!groupsOnly) },
                        )
                    }
                    Box(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                        MapIconButton(DS.drawable.ic_my_location, stringResource(R.string.map_centre_on_me), ::centreOnMe)
                        ExtendedFloatingActionButton(
                            onClick = { if (entry == null) showGetMap = true else picking = true },
                            icon = { Icon(painterResource(DS.drawable.ic_add_location_alt), contentDescription = null) },
                            text = { Text(stringResource(R.string.map_report), style = MaterialTheme.typography.labelLarge) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            // A minimum, not a fixed size: a longer label (French, large text) must
                            // grow the button, or it is clipped and TalkBack finds nothing to read.
                            modifier = Modifier.sizeIn(minWidth = Touch.hero + Touch.hero / 2, minHeight = Touch.hero),
                        )
                    }
                }
            }
        }

        if (picking && entry != null) {
            ReportPicker(
                chosen = chosen,
                nextAllowed = mesh::nextAllowed,
                now = now,
                onChoose = { chosen = it },
                onSend = { type ->
                    val above = constraints.maxHeight - pickerHeight
                    val at = camera.fromScreen(Offset(constraints.maxWidth / 2f, above / 2f))
                    val to = audience
                    if (at != null && mesh.send(type, at, group = to)) {
                        picking = false
                        chosen = null
                        scope.launch(Dispatchers.IO) {
                            app.settings.update { it.copy(lastAudienceIsPublic = to == null, lastAudienceGroup = to?.hex()) }
                        }
                    }
                },
                onClose = {
                    picking = false
                    chosen = null
                },
                groups = groups,
                audience = audience,
                onAudience = { audience = it },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { pickerHeight = it.height },
            )
        }
    }

    detailFor?.let { shown ->
        // Re-read it, so a report that expires or is withdrawn while open closes its sheet.
        val live = reports.firstOrNull { it.id == shown.id }
        if (live == null) {
            detailFor = null
        } else {
            ReportDetailSheet(
                shown = live,
                from = ownFix?.point() ?: camera.center(),
                now = now,
                onWithdraw = {
                    scope.launch {
                        if (mesh.withdraw(live.id)) snackbar.showSnackbar(withdrawnMessage)
                        detailFor = null
                    }
                },
                onVote = { vote -> scope.launch { mesh.vote(live.id, vote) } },
                onDismiss = { detailFor = null },
            )
        }
    }

    if (showMeshSheet) {
        MeshSheet(
            state = meshState,
            radios = app.transports.map { it.capabilities },
            simulatedRadio = app.simulatedRadio,
            now = now,
            meantOn = settings.meshMeantOn,
            onToggle = ::toggleMesh,
            onFix = ::fix,
            onDismiss = { showMeshSheet = false },
            diagnosticsOn = diagnosticsOn,
            onOpenDiagnostics = {
                showMeshSheet = false
                onOpenDiagnostics()
            },
        )
    }

    if (showLocationSheet) {
        LocationSheet(
            onContinue = {
                showLocationSheet = false
                locationAsked = true
                locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onKeepOff = {
                showLocationSheet = false
                locationAsked = true
            },
        )
    }

    if (showNearbySheet) {
        PermissionSheet(
            icon = DS.drawable.ic_radar,
            title = R.string.nearby_title,
            body = R.string.nearby_body,
            onContinue = {
                showNearbySheet = false
                app.settings.update { it.copy(nearbyAsked = true) }
                nearbyLauncher.launch(app.meshPermissions.toTypedArray())
            },
            onNotNow = {
                showNearbySheet = false
                switchMesh(true)
            },
        )
    }

    if (showNotificationsSheet) {
        PermissionSheet(
            icon = DS.drawable.ic_info,
            title = R.string.notifications_title,
            body = R.string.notifications_body,
            onContinue = {
                showNotificationsSheet = false
                app.settings.update { it.copy(notificationsAsked = true) }
                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onNotNow = {
                showNotificationsSheet = false
                app.settings.update { it.copy(notificationsAsked = true) }
                switchMesh(true)
            },
        )
    }

    if (showGetMap) {
        GetMapSheet(maps, onResult = { scope.launch { snackbar.showSnackbar(it) } }, onDismiss = { showGetMap = false })
    }
}

private const val MILLIS_PER_SECOND = 1000.0

private fun granted(
    context: Context,
    permission: String,
): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Echo's page in Android's Settings, where a refused permission is granted after the dialog. */
private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }
}

private fun hasLocation(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Every held report as a pin over the map, hazards drawn above blocked above safe. Pins off screen
 * are skipped. Clustering below zoom 16 comes with the street render theme.
 */
@Composable
private fun Pins(
    reports: List<ShownReport>,
    camera: MapCamera,
    now: Instant,
    onClick: (ShownReport) -> Unit,
) {
    val half = with(LocalDensity.current) { (Touch.min / 2).roundToPx() }
    val placed =
        reports
            .filter { now < it.report.expiresAt }
            .sortedByDescending { it.report.type.family.ordinal }
            .mapNotNull { shown -> camera.toScreen(shown.report.position)?.let { shown to it } }
    // A pin drawn over most of another hides it from TalkBack as well as from the eye: the top pin
    // of a stack speaks for the stack and offers every report under it as an action (NFR-11).
    val coveredBy = HashMap<Int, Int>()
    placed.indices.forEach { i ->
        val (_, a) = placed[i]
        val top = (placed.lastIndex downTo i + 1).firstOrNull { j -> (placed[j].second - a).getDistance() < half }
        if (top != null) coveredBy[i] = top
    }

    fun topOf(i: Int): Int = coveredBy[i]?.let(::topOf) ?: i
    val under = placed.indices.filter { it in coveredBy }.groupBy(::topOf)
    val stackLabel = stringResource(R.string.map_pins_stacked)
    val openLabel = stringResource(R.string.map_pin_open)
    Box(Modifier.fillMaxSize()) {
        placed.forEachIndexed { i, (shown, at) ->
            val hidden = under[i].orEmpty().map { placed[it].first }
            val said = hidden.map { it to it.describe(now) }
            val description =
                if (hidden.isEmpty()) {
                    shown.describe(now)
                } else {
                    (listOf(stackLabel.format(hidden.size + 1), shown.describe(now)) + said.map { it.second }).joinToString(". ")
                }
            val semantics =
                when {
                    i in coveredBy -> {
                        Modifier.clearAndSetSemantics {}
                    }

                    hidden.isEmpty() -> {
                        Modifier
                    }

                    else -> {
                        Modifier.semantics {
                            customActions =
                                said.map { (other, words) ->
                                    CustomAccessibilityAction(openLabel.format(words)) {
                                        onClick(other)
                                        true
                                    }
                                }
                        }
                    }
                }
            ReportPin(
                look = shown.look(now),
                contentDescription = description,
                onClick = { onClick(shown) },
                modifier = Modifier.offset { IntOffset(at.x.toInt() - half, at.y.toInt() - half) }.then(semantics),
            )
        }
    }
}

/** Group members' latest shared positions (FR-10.3); an expired one is not drawn at all. */
@Composable
private fun Members(
    positions: List<MemberPosition>,
    camera: MapCamera,
    now: Instant,
) {
    val groups = LocalGroups.current
    val half = with(LocalDensity.current) { (MARKER_DP.dp / 2).roundToPx() }
    Box(Modifier.fillMaxSize()) {
        positions.filter { now < it.expiresAt }.forEach { member ->
            val at = camera.toScreen(member.position) ?: return@forEach
            MemberMarker(
                member,
                groups.colourOf(member.groupId),
                now,
                Modifier.offset { IntOffset(at.x.toInt() - half, at.y.toInt() - half) },
            )
        }
    }
}

private fun crosshairHalf(density: Float): IntOffset {
    val half = (CROSSHAIR_DP / 2 * density).toInt()
    return IntOffset(half, half)
}

private const val CROSSHAIR_DP = 48

/** Where the report will go: the centre of the map above the picker (FR-4.2). */
@Composable
private fun Crosshair(modifier: Modifier) {
    val description = stringResource(R.string.map_crosshair)
    val color = MaterialTheme.colorScheme.primary
    val ring = MaterialTheme.colorScheme.surface
    Canvas(modifier.size(CROSSHAIR_DP.dp).semantics { contentDescription = description }) {
        val c = center
        val arm = size.minDimension / 2
        listOf(Offset(arm, 0f), Offset(0f, arm)).forEach { d ->
            drawLine(ring, c - d, c + d, strokeWidth = 6.dp.toPx())
            drawLine(color, c - d, c + d, strokeWidth = 3.dp.toPx())
        }
        drawCircle(color, radius = 4.dp.toPx(), center = c)
    }
}

/** No map loaded (screens/MapView): blank land with a faint grid and a banner to get one. */
@Composable
private fun NoMap(onGetMap: () -> Unit) {
    val street = LocalStreetColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(street.mapLand),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = GRID_DP.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(street.mapRoadMinor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(street.mapRoadMinor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(LocalSpacing.current.xxl),
        ) {
            Row(Modifier.padding(start = LocalSpacing.current.lg), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.map_no_map), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onGetMap, modifier = Modifier.size(width = Touch.hero + Touch.min, height = Touch.min)) {
                    Text(stringResource(R.string.map_get_map), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private const val GRID_DP = 48

/** A 64dp filled icon button for the map's corners (components/IconButton). */
@Composable
private fun MapIconButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = Modifier.size(Touch.min),
    ) {
        Icon(painterResource(icon), contentDescription = description)
    }
}

/** Location, asked in context with its reason, our sheet first (patterns/PermissionsInContext). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSheet(
    onContinue: () -> Unit,
    onKeepOff: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onKeepOff) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Icon(painterResource(DS.drawable.ic_my_location), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.location_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.location_body), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(stringResource(R.string.continue_), onContinue)
            SecondaryButton(stringResource(R.string.location_keep_off), onKeepOff)
        }
    }
}

/** One permission, one sentence of why; only Continue shows Android's dialog (patterns/PermissionsInContext). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionSheet(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes body: Int,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onNotNow) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(stringResource(R.string.continue_), onContinue)
            SecondaryButton(stringResource(R.string.not_now), onNotNow)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GetMapSheet(
    maps: MapsState,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(stringResource(R.string.get_map_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.onboarding_map_body), style = MaterialTheme.typography.bodyLarge)
            GetMapChoices(
                maps,
                onResult = {
                    onResult(it)
                    onDismiss()
                },
            )
        }
    }
}

private fun OwnFix.point() = GeoPoint(latitude, longitude)
