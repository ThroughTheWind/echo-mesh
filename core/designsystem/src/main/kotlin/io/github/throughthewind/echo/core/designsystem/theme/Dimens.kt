package io.github.throughthewind.echo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Zapper's 4dp spacing scale, unchanged. */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    /** Also the minimum gap between two touch targets. */
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
    val giant: Dp = 64.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Street sizes (tokens.json > size). Larger than Zapper's: gloves, cold hands, one hand. */
object Touch {
    /** Smallest hit area of anything tappable, pins included. Zapper: 56dp. */
    val min = 64.dp

    /** Buttons in sheets, list rows, picker tiles. */
    val comfort = 72.dp

    /** The Report button and the hold-to-wipe button. */
    val hero = 88.dp

    /** Fraction of the screen height, from the bottom, holding every frequent action. */
    const val THUMB_ZONE = 0.45f
}

@Immutable
data class StreetSizes(
    val pin: Dp,
    val pinHalo: Dp = 3.dp,
    val expiryRing: Dp = 4.dp,
    val iconPin: Dp = 24.dp,
) {
    companion object {
        val Standard = StreetSizes(pin = 44.dp)
        val Large = StreetSizes(pin = 56.dp)
    }
}

val LocalStreetSizes = staticCompositionLocalOf { StreetSizes.Standard }

object TopBar {
    val height = 64.dp
}

val StreetShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/** Pin shapes carry meaning (one per report family) and are not user-selectable. */
object PinShapes {
    /** hazard: a 36dp square rotated 45 degrees. */
    val hazard = RoundedCornerShape(6.dp)

    /** blocked: a 40dp rounded square. */
    val blocked = RoundedCornerShape(8.dp)

    /** safe: a 44dp circle. */
    val safe = RoundedCornerShape(percent = 50)
}
