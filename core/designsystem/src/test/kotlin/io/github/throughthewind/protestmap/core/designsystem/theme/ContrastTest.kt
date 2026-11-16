package io.github.throughthewind.protestmap.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

/**
 * Street contrast in every bundled theme, against the Compose colours the app actually draws:
 * text 7:1 (AAA) in Dark and Sun, 4.5:1 in Discreet (deliberately dim), 3:1 for pins, halos and
 * outlines against the map. Mirrors `docs/design/tools/tokens.py check`, which gates tokens.json.
 */
class ContrastTest {
    private class Pick(
        val name: String,
        val pick: (ColorScheme, StreetColors) -> Pair<Color, Color>,
    )

    private val text =
        listOf(
            Pick("onSurface on surface") { s, _ -> s.onSurface to s.surface },
            Pick("onSurface on a card") { s, _ -> s.onSurface to s.surfaceContainerHigh },
            Pick("onSurfaceVariant on surface") { s, _ -> s.onSurfaceVariant to s.surface },
            Pick("onSurfaceVariant on a card") { s, _ -> s.onSurfaceVariant to s.surfaceContainerHigh },
            Pick("onPrimary on the Report button") { s, _ -> s.onPrimary to s.primary },
            Pick("onPrimaryContainer on the sharing bar") { s, _ -> s.onPrimaryContainer to s.primaryContainer },
            Pick("onSecondaryContainer on a tonal button") { s, _ -> s.onSecondaryContainer to s.secondaryContainer },
            Pick("onError on the wipe fill") { s, _ -> s.onError to s.error },
            Pick("onErrorContainer on the wipe button") { s, _ -> s.onErrorContainer to s.errorContainer },
            Pick("primary text on surface") { s, _ -> s.primary to s.surface },
            Pick("icon on a hazard pin") { _, c -> c.onHazard to c.hazard },
            Pick("icon on a blocked pin") { _, c -> c.onBlocked to c.blocked },
            Pick("icon on a safe pin") { _, c -> c.onSafe to c.safe },
            Pick("hazard tile text") { _, c -> c.onHazardContainer to c.hazardContainer },
            Pick("blocked tile text") { _, c -> c.onBlockedContainer to c.blockedContainer },
            Pick("safe tile text") { _, c -> c.onSafeContainer to c.safeContainer },
            Pick("street label on the map") { _, c -> c.mapLabel to c.mapLand },
        )

    private val marks =
        listOf(
            Pick("outline on surface") { s, _ -> s.outline to s.surface },
            Pick("you on the map") { s, c -> s.primary to c.mapLand },
            Pick("you on a main road") { s, c -> s.primary to c.mapRoadMajor },
        ) +
            listOf<Pair<String, (StreetColors) -> Color>>(
                "hazard" to { it.hazard },
                "blocked" to { it.blocked },
                "safe" to { it.safe },
            ).flatMap { (n, f) ->
                listOf(
                    Pick("$n pin on the map") { _, c -> f(c) to c.mapLand },
                    Pick("$n pin on a main road") { _, c -> f(c) to c.mapRoadMajor },
                    Pick("halo around a $n pin") { _, c -> c.pinHalo to f(c) },
                )
            } +
            listOf<(StreetColors) -> Color>({ it.group1 }, { it.group2 }, { it.group3 }, { it.group4 }, { it.group5 })
                .mapIndexed { i, f -> Pick("group ${i + 1} halo on the map") { _, c -> f(c) to c.mapLand } }

    @Test
    fun `every bundled theme meets street contrast`() {
        val failures =
            StreetMode.entries.flatMap { mode ->
                val scheme = schemeFor(mode)
                val street = streetColorsFor(mode)
                val textMin = if (mode == StreetMode.Discreet) 4.5 else 7.0
                (text.mapNotNull { below(it, scheme, street, textMin) } + marks.mapNotNull { below(it, scheme, street, 3.0) })
                    .map { "$mode, $it" }
            }
        assertTrue(failures.isEmpty(), "Below street contrast:\n" + failures.joinToString("\n"))
    }

    private fun below(
        p: Pick,
        scheme: ColorScheme,
        street: StreetColors,
        minimum: Double,
    ): String? {
        val (fg, bg) = p.pick(scheme, street)
        val ratio = contrast(fg, bg)
        return if (ratio < minimum) "${p.name}: ${"%.2f".format(ratio)} < $minimum" else null
    }

    /** WCAG 2.x contrast ratio, computed from the sRGB channels. */
    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(c: Color): Double {
        fun lin(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    }
}
