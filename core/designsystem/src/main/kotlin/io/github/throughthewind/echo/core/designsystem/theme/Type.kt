package io.github.throughthewind.echo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Roboto Flex goes here once res/font/roboto_flex.ttf (OFL-1.1) is bundled: then build this from
 * Font(R.font.roboto_flex, weight, FontVariation.Settings(FontVariation.weight(w))). Never the
 * downloadable-fonts provider: it goes through Google Play Services and the network (constraints 2
 * and 3). Until then the system sans keeps the build green.
 */
private val Brand: FontFamily = FontFamily.Default

/** Sunlight adds 100 to every weight: glare erases thin strokes first. */
private const val SUN_WEIGHT_BOOST = 100

private fun style(
    size: Int,
    line: Int,
    weight: Int,
    tracking: Double,
    sun: Boolean,
): TextStyle {
    val w = (weight + if (sun) SUN_WEIGHT_BOOST else 0).coerceAtMost(1000)
    return TextStyle(
        fontFamily = Brand,
        fontWeight = FontWeight(w),
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = (tracking / size).em,
    )
}

/** The three glance styles that M3's scale doesn't have. */
@Immutable
data class GlanceType(
    val numeral: TextStyle,
    val title: TextStyle,
    val label: TextStyle,
)

fun glanceType(sun: Boolean) =
    GlanceType(
        numeral = style(40, 44, 700, -0.5, sun).copy(fontFeatureSettings = "tnum"),
        title = style(24, 30, 700, 0.0, sun),
        label = style(18, 22, 700, 0.1, sun),
    )

val LocalGlanceType = staticCompositionLocalOf { glanceType(sun = false) }

/** Zapper's M3 scale shifted up one step. Nothing below 13sp; no sentence below 16sp. */
fun streetTypography(sun: Boolean) =
    Typography(
        headlineMedium = style(28, 36, 600, 0.0, sun),
        headlineSmall = style(24, 32, 600, 0.0, sun),
        titleLarge = style(22, 28, 600, 0.0, sun),
        titleMedium = style(18, 24, 600, 0.1, sun),
        titleSmall = style(16, 22, 600, 0.1, sun),
        bodyLarge = style(18, 26, 400, 0.2, sun),
        bodyMedium = style(16, 24, 400, 0.2, sun),
        bodySmall = style(16, 24, 400, 0.2, sun),
        labelLarge = style(16, 20, 700, 0.2, sun),
        labelMedium = style(14, 18, 700, 0.3, sun),
        labelSmall = style(13, 16, 700, 0.5, sun),
    )
