package io.github.throughthewind.echo.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import io.github.throughthewind.echo.ui.MapsState
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import io.github.throughthewind.echo.ui.describe
import io.github.throughthewind.echo.ui.family
import io.github.throughthewind.echo.ui.look
import io.github.throughthewind.echo.ui.maps.GetMapChoices
import io.github.throughthewind.echo.ui.rememberNow
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
    onOpenList: () -> Unit,
    onOpenProtects: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mesh = app.mesh
    val meshState by mesh.state.collectAsStateWithLifecycle()
    val reports by mesh.reports.collectAsStateWithLifecycle()
    val pending by mesh.pending.collectAsStateWithLifecycle()
    val now = rememberNow(if (pending != null) 250.milliseconds else 1000.milliseconds)
    val camera = rememberMapCamera()
    val entry = maps.current
    val snackbar = remember { SnackbarHostState() }

    var locationGranted by remember { mutableStateOf(hasLocation(context)) }
    var locationAsked by rememberSaveable { mutableStateOf(false) }
    var showLocationSheet by rememberSaveable { mutableStateOf(false) }
    val ownFix by remember(locationGranted) {
        if (locationGranted) OwnPosition.updates(context) else emptyFlow()
    }.collectAsStateWithLifecycle(null)

    var picking by rememberSaveable { mutableStateOf(false) }
    var chosen by rememberSaveable { mutableStateOf<ReportType?>(null) }
    var detailFor by remember { mutableStateOf<ShownReport?>(null) }
    var showMeshSheet by rememberSaveable { mutableStateOf(false) }
    var showGetMap by rememberSaveable { mutableStateOf(false) }
    var pickerHeight by remember { mutableIntStateOf(0) }

    val sentMessage = stringResource(R.string.sent)
    val meshOnMessage = stringResource(R.string.mesh_on_snackbar)
    val meshOffMessage = stringResource(R.string.mesh_off_snackbar)
    val withdrawnMessage = stringResource(R.string.detail_withdrawn)
    val locationRefused = stringResource(R.string.location_refused)
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

    val locationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            locationGranted = result.values.any { it }
            if (!locationGranted) scope.launch { snackbar.showSnackbar(locationRefused) }
        }

    fun centreOnMe() {
        val fix = ownFix
        when {
            fix != null -> camera.centreOn(fix.point())
            locationGranted -> Unit
            locationAsked -> scope.launch { snackbar.showSnackbar(locationRefused) }
            else -> showLocationSheet = true
        }
    }

    fun toggleMesh(on: Boolean) {
        scope.launch {
            if (on) mesh.turnOn() else mesh.turnOff()
            snackbar.showSnackbar(if (on) meshOnMessage else meshOffMessage)
        }
    }

    val listLabel = stringResource(R.string.map_reports_near_you)
    val mapDescription = stringResource(R.string.map_described, reports.size)
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
            Pins(reports, camera, now, onClick = { detailFor = it })
        } else {
            NoMap(onGetMap = { showGetMap = true })
        }

        if (picking && entry != null) {
            val above = constraints.maxHeight - pickerHeight
            Crosshair(Modifier.offset { IntOffset(constraints.maxWidth / 2, above / 2) - crosshairHalf(density.density) })
        }

        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = LocalSpacing.current.lg)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MeshStatusChip(
                state = meshState.chip(now),
                onClick = { if (meshState == MeshState.Off) toggleMesh(true) else showMeshSheet = true },
            )
            Box(Modifier.weight(1f))
            MapIconButton(DS.drawable.ic_shield, stringResource(R.string.protects_title), onOpenProtects)
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
                    Box(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                        MapIconButton(DS.drawable.ic_my_location, stringResource(R.string.map_centre_on_me), ::centreOnMe)
                        ExtendedFloatingActionButton(
                            onClick = { if (entry == null) showGetMap = true else picking = true },
                            icon = { Icon(painterResource(DS.drawable.ic_add_location_alt), contentDescription = null) },
                            text = { Text(stringResource(R.string.map_report), style = MaterialTheme.typography.labelLarge) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(width = Touch.hero + Touch.hero / 2, height = Touch.hero),
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
                    if (at != null && mesh.send(type, at)) {
                        picking = false
                        chosen = null
                    }
                },
                onClose = {
                    picking = false
                    chosen = null
                },
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
            onToggle = ::toggleMesh,
            onDismiss = { showMeshSheet = false },
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

    if (showGetMap) {
        GetMapSheet(maps, onResult = { scope.launch { snackbar.showSnackbar(it) } }, onDismiss = { showGetMap = false })
    }
}

private const val MILLIS_PER_SECOND = 1000.0

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
    Box(Modifier.fillMaxSize()) {
        reports
            .filter { now < it.report.expiresAt }
            .sortedByDescending { it.report.type.family.ordinal }
            .forEach { shown ->
                val at = camera.toScreen(shown.report.position) ?: return@forEach
                ReportPin(
                    look = shown.look(now),
                    contentDescription = shown.describe(now),
                    onClick = { onClick(shown) },
                    modifier = Modifier.offset { IntOffset(at.x.toInt() - half, at.y.toInt() - half) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GetMapSheet(
    maps: MapsState,
    onResult: (String) -> Unit,
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
