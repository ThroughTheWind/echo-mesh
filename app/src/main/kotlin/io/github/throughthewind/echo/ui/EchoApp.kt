package io.github.throughthewind.echo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.ui.diagnostics.DiagnosticsScreen
import io.github.throughthewind.echo.ui.groups.GroupsScreen
import io.github.throughthewind.echo.ui.groups.ShareOwnPosition
import io.github.throughthewind.echo.ui.map.MapScreen
import io.github.throughthewind.echo.ui.onboarding.Onboarding
import io.github.throughthewind.echo.ui.protects.WhatEchoProtects
import io.github.throughthewind.echo.ui.reports.ReportsNearYou
import io.github.throughthewind.echo.ui.settings.SettingsScreen
import io.github.throughthewind.echo.ui.wipe.LocalPanicWipe
import io.github.throughthewind.echo.ui.wipe.PanicWipeSheet
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The top-level screens. No navigation library: a few screens fit in a `when`. */
private enum class Screen { Map, Reports, Protects, Groups, Diagnostics, Settings }

/**
 * The app's screens. First launch, and the first launch after a panic wipe, show onboarding before
 * anything else and before any permission (FR-1.1, AC-1).
 */
@Composable
fun EchoApp(app: App) {
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val maps = remember { MapsState(app.maps) }
    if (!settings.onboardingDone) {
        Onboarding(maps = maps, onDone = { app.settings.update { it.copy(onboardingDone = true) } })
        return
    }
    val groups by app.mesh.groups.collectAsStateWithLifecycle()
    ShareOwnPosition(app)
    var wiping by rememberSaveable { mutableStateOf(false) }
    CompositionLocalProvider(LocalGroups provides groups, LocalPanicWipe provides { wiping = true }) { Screens(app, maps) }
    if (wiping) PanicWipeSheet(app, onDismiss = { wiping = false })
}

@Composable
private fun Screens(
    app: App,
    maps: MapsState,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.Map) }
    // Opening the report picker from the list returns to the map with it open.
    var pickOnMap by rememberSaveable { mutableStateOf(false) }
    // Turning the mesh on from Settings returns to the map, which asks for permissions in context.
    var meshOnFromSettings by rememberSaveable { mutableStateOf(false) }
    // Where Back goes from a screen Settings opened: back to Settings, otherwise to the map.
    var fromSettings by rememberSaveable { mutableStateOf(false) }
    val back = { screen = if (fromSettings && screen != Screen.Settings) Screen.Settings else Screen.Map }

    fun open(next: Screen) {
        fromSettings = screen == Screen.Settings || (fromSettings && next != Screen.Map)
        screen = next
    }
    // Everything, or my groups only: the same filter on the map and the list (FR-9.3).
    var groupsOnly by rememberSaveable { mutableStateOf(false) }
    // A member's shared position to centre the map on, from the roster (screens/GroupRoster).
    var focus by remember { mutableStateOf<GeoPoint?>(null) }
    val groups = LocalGroups.current
    LaunchedEffect(groups.isEmpty()) { if (groups.isEmpty()) groupsOnly = false }
    BackHandler(enabled = screen != Screen.Map) { back() }
    LaunchedEffect(screen) { if (screen == Screen.Map) fromSettings = false }
    when (screen) {
        Screen.Map -> {
            MapScreen(
                app = app,
                maps = maps,
                openPicker = pickOnMap,
                onPickerOpened = { pickOnMap = false },
                meshOnRequested = meshOnFromSettings,
                onMeshOnHandled = { meshOnFromSettings = false },
                onOpenList = { open(Screen.Reports) },
                onOpenSettings = { open(Screen.Settings) },
                onOpenDiagnostics = { open(Screen.Diagnostics) },
                onOpenGroups = { open(Screen.Groups) },
                groupsOnly = groupsOnly,
                onGroupsOnly = { groupsOnly = it },
                focus = focus,
                onFocused = { focus = null },
            )
        }

        Screen.Reports -> {
            ReportsNearYou(
                app = app,
                maps = maps,
                onBack = { screen = Screen.Map },
                onReportHere = {
                    pickOnMap = true
                    screen = Screen.Map
                },
                groupsOnly = groupsOnly,
                onGroupsOnly = { groupsOnly = it },
            )
        }

        Screen.Protects -> {
            WhatEchoProtects(onBack = back)
        }

        Screen.Diagnostics -> {
            DiagnosticsScreen(app, onBack = back)
        }

        Screen.Settings -> {
            SettingsScreen(
                app = app,
                maps = maps,
                onBack = back,
                onTurnMeshOn = {
                    meshOnFromSettings = true
                    screen = Screen.Map
                },
                onOpenGroups = { open(Screen.Groups) },
                onOpenProtects = { open(Screen.Protects) },
                onOpenDiagnostics = { open(Screen.Diagnostics) },
            )
        }

        Screen.Groups -> {
            GroupsScreen(
                app = app,
                onBack = back,
                onShowOnMap = {
                    focus = it
                    screen = Screen.Map
                },
            )
        }
    }
}

/** The current time, refreshed every [period]: minutes on pins, seconds on the mesh chip. */
@Composable
fun rememberNow(period: Duration = 1.seconds): Instant {
    val now by produceState(Clock.System.now(), period) {
        while (true) {
            delay(period)
            value = Clock.System.now()
        }
    }
    return now
}
