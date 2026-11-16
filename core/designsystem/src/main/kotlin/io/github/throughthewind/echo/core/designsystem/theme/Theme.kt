package io.github.throughthewind.echo.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** The three bundled themes. Unlike Zapper there is no dynamic colour and no seed preset: pins must look the same on every phone. */
enum class StreetMode { Dark, Sun, Discreet }

enum class ThemePref { Auto, Dark, Sun }

enum class HapticsLevel { Off, Light, Strong }

/** Everything the user can change in Settings > Display and Feedback. Persist this (DataStore). */
@Immutable
data class DisplayPrefs(
    val theme: ThemePref = ThemePref.Auto,
    /** One tap on the map's top bar or the Quick Settings tile. Overrides theme, haptics and large pins. */
    val discreet: Boolean = false,
    val largePins: Boolean = false,
    val haptics: HapticsLevel = HapticsLevel.Light,
    val nearbyHazardBuzz: Boolean = false,
)

/** Which theme applies, given the prefs and whether the ambient light sensor currently reads outdoor sun (the caller owns the sensor and its hysteresis, see theming-and-customisation.md). */
fun resolveMode(
    prefs: DisplayPrefs,
    bright: Boolean,
): StreetMode =
    when {
        prefs.discreet -> StreetMode.Discreet
        prefs.theme == ThemePref.Sun -> StreetMode.Sun
        prefs.theme == ThemePref.Auto && bright -> StreetMode.Sun
        else -> StreetMode.Dark
    }

fun schemeFor(mode: StreetMode): ColorScheme =
    when (mode) {
        StreetMode.Dark -> darkScheme
        StreetMode.Sun -> sunScheme
        StreetMode.Discreet -> discreetScheme
    }

fun streetColorsFor(mode: StreetMode): StreetColors =
    when (mode) {
        StreetMode.Dark -> darkStreetColors
        StreetMode.Sun -> sunStreetColors
        StreetMode.Discreet -> discreetStreetColors
    }

val LocalStreetColors = staticCompositionLocalOf { darkStreetColors }
val LocalStreetMode = staticCompositionLocalOf { StreetMode.Dark }

/** Effective haptics: discreet mode forces Off (SPEC FR-13.1). */
val LocalHaptics = staticCompositionLocalOf { HapticsLevel.Light }

/** Window brightness while discreet mode is on and the app is in front (token discreet-brightness). */
const val DISCREET_BRIGHTNESS = 0.12f

@Composable
fun StreetTheme(
    prefs: DisplayPrefs = DisplayPrefs(),
    bright: Boolean = false,
    content: @Composable () -> Unit,
) {
    val mode = resolveMode(prefs, bright)
    DiscreetBrightness(enabled = mode == StreetMode.Discreet)
    CompositionLocalProvider(
        LocalStreetColors provides streetColorsFor(mode),
        LocalStreetMode provides mode,
        LocalHaptics provides if (mode == StreetMode.Discreet) HapticsLevel.Off else prefs.haptics,
        LocalSpacing provides Spacing(),
        LocalGlanceType provides glanceType(sun = mode == StreetMode.Sun),
        LocalStreetSizes provides if (prefs.largePins && !prefs.discreet) StreetSizes.Large else StreetSizes.Standard,
    ) {
        MaterialTheme(
            colorScheme = schemeFor(mode),
            typography = streetTypography(sun = mode == StreetMode.Sun),
            shapes = StreetShapes,
            content = content,
        )
    }
}

/** Dims the window in discreet mode and restores the system brightness when it ends. */
@Composable
private fun DiscreetBrightness(enabled: Boolean) {
    val window = (LocalContext.current as? Activity)?.window ?: return
    DisposableEffect(enabled) {
        val params = window.attributes
        val before = params.screenBrightness
        if (enabled) {
            params.screenBrightness = DISCREET_BRIGHTNESS
            window.attributes = params
        }
        onDispose {
            params.screenBrightness = before
            window.attributes = params
        }
    }
}
