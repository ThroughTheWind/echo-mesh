package io.github.throughthewind.echo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.ui.groups.GroupsScreen
import io.github.throughthewind.echo.ui.map.MapScreen
import io.github.throughthewind.echo.ui.onboarding.Onboarding
import io.github.throughthewind.echo.ui.protects.WhatEchoProtects
import io.github.throughthewind.echo.ui.reports.ReportsNearYou
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The top-level screens. No navigation library: a few screens fit in a `when`. */
private enum class Screen { Map, Reports, Protects, Groups }

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
    CompositionLocalProvider(LocalGroups provides groups) { Screens(app, maps) }
}

@Composable
private fun Screens(
    app: App,
    maps: MapsState,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.Map) }
    // Opening the report picker from the list returns to the map with it open.
    var pickOnMap by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = screen != Screen.Map) { screen = Screen.Map }
    when (screen) {
        Screen.Map -> {
            MapScreen(
                app = app,
                maps = maps,
                openPicker = pickOnMap,
                onPickerOpened = { pickOnMap = false },
                onOpenList = { screen = Screen.Reports },
                onOpenProtects = { screen = Screen.Protects },
                onOpenGroups = { screen = Screen.Groups },
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
            )
        }

        Screen.Protects -> {
            WhatEchoProtects(onBack = { screen = Screen.Map })
        }

        Screen.Groups -> {
            GroupsScreen(app = app, onBack = { screen = Screen.Map })
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
