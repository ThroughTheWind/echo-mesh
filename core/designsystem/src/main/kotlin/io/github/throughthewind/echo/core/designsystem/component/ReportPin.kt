package io.github.throughthewind.echo.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetSizes
import io.github.throughthewind.echo.core.designsystem.theme.StreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch

/** A report family: fixed colour and shape, never user-selectable (foundations/Colour). */
enum class PinFamily { Hazard, Blocked, Safe }

/** Where a report came from: one of the user's groups (signed by a member) or any phone nearby. */
sealed interface PinSource {
    data object Nearby : PinSource

    /** A group report: [colorIndex] picks `group-1` to `group-5`; [monogram] is its badge. */
    data class Group(
        val colorIndex: Int,
        val monogram: String,
    ) : PinSource
}

/** Everything a pin shows (docs/design/components/ReportPin). */
@Immutable
data class PinLook(
    val family: PinFamily,
    @param:DrawableRes val icon: Int,
    val source: PinSource,
    /** Time left over the current lifetime, 0 to 1. */
    val timeLeft: Float,
    val confirmations: Int = 0,
    val probablyGone: Boolean = false,
    val mine: Boolean = false,
    /** A public steering report from another phone: marked "could be a trap" (extended spec FR-15.2). */
    val couldBeATrap: Boolean = false,
)

/** Visual sizes from the component spec, scaled with the Large pins setting. */
private object PinGeometry {
    const val HAZARD = 36f / 44f
    const val BLOCKED = 40f / 44f
    val dash = floatArrayOf(6f, 4f)
    const val MINE_GAP = 10f / 44f
}

/**
 * A report on the map: family shape and colour, type icon, time-left ring, and solid (a group's,
 * verified) or hollow with a dashed edge (anyone nearby, unverified). The hit area is always
 * [Touch.min]. [contentDescription] is the whole pin read aloud: "{Type}, {source}, {N} minutes
 * left[, {N} confirmations][, probably gone]".
 */
@Composable
fun ReportPin(
    look: PinLook,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStreetColors.current
    val sizes = LocalStreetSizes.current
    val track = MaterialTheme.colorScheme.surface
    val ring = MaterialTheme.colorScheme.onSurface
    val mineColor = MaterialTheme.colorScheme.primary
    val (fill, edge, onFill) =
        familyColors(colors, look.family, solid = look.source is PinSource.Group).let { (f, e, o) ->
            if (look.probablyGone) Triple(f.greyed(), e.greyed(), o.greyed()) else Triple(f, e, o)
        }
    val halo =
        when (val source = look.source) {
            is PinSource.Group -> groupColor(colors, source.colorIndex)
            PinSource.Nearby -> colors.pinHalo
        }
    Box(
        modifier
            .size(Touch.min)
            .then(if (look.probablyGone) Modifier.alpha(PROBABLY_GONE_ALPHA) else Modifier)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pin = sizes.pin.toPx()
            val haloWidth = sizes.pinHalo.toPx()
            val ringWidth = sizes.expiryRing.toPx()
            val ringRadius = pin / 2 + haloWidth + ringWidth / 2 + 1.dp.toPx()
            drawExpiryRing(look.timeLeft.coerceIn(0f, 1f), ringRadius, ringWidth, ring, track)
            drawPinShape(look.family, pin + 2 * haloWidth, halo, null)
            drawPinShape(look.family, pin, fill, if (look.source is PinSource.Group) null else edge)
            if (look.mine) {
                drawPinShape(look.family, pin + 2 * pin * PinGeometry.MINE_GAP, mineColor, null, outline = 2.dp.toPx())
            }
        }
        Icon(
            painter = painterResource(look.icon),
            contentDescription = null,
            tint = onFill,
            modifier = Modifier.size(sizes.iconPin),
        )
        if (look.source is PinSource.Group) {
            Badge(
                text = look.source.monogram,
                background = groupColor(colors, look.source.colorIndex),
                content = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
        if (look.couldBeATrap) {
            // An amber "?" at the bottom: unconfirmed, whatever its votes say (FR-15.2).
            Badge(
                text = "?",
                background = colors.blocked,
                content = colors.onBlocked,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        if (look.confirmations >= 2) {
            Badge(
                text = look.confirmations.toString(),
                background = MaterialTheme.colorScheme.inverseSurface,
                content = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/** A probably-gone pin's or row's opacity (token `probably-gone`, FR-6.4). */
const val PROBABLY_GONE_ALPHA = 0.45f

/** 60% of the way to grey, for probably-gone pins (FR-6.4). */
fun Color.greyed(): Color {
    val grey = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return lerp(this, Color(grey, grey, grey, alpha), GREYSCALE)
}

private const val GREYSCALE = 0.6f

@Composable
private fun Badge(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = background, contentColor = content, shape = CircleShape, modifier = modifier.padding(2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
    }
}

/** Fill, edge and icon colour: solid family colour for group reports, the tonal container otherwise. */
private fun familyColors(
    colors: StreetColors,
    family: PinFamily,
    solid: Boolean,
): Triple<Color, Color, Color> =
    when (family) {
        PinFamily.Hazard -> {
            if (solid) {
                Triple(
                    colors.hazard,
                    colors.hazard,
                    colors.onHazard,
                )
            } else {
                Triple(colors.hazardContainer, colors.hazard, colors.onHazardContainer)
            }
        }

        PinFamily.Blocked -> {
            if (solid) {
                Triple(
                    colors.blocked,
                    colors.blocked,
                    colors.onBlocked,
                )
            } else {
                Triple(colors.blockedContainer, colors.blocked, colors.onBlockedContainer)
            }
        }

        PinFamily.Safe -> {
            if (solid) {
                Triple(
                    colors.safe,
                    colors.safe,
                    colors.onSafe,
                )
            } else {
                Triple(colors.safeContainer, colors.safe, colors.onSafeContainer)
            }
        }
    }

/** `group-1` to `group-5`. */
fun groupColor(
    colors: StreetColors,
    index: Int,
): Color =
    when (Math.floorMod(index, GROUP_COLOURS)) {
        0 -> colors.group1
        1 -> colors.group2
        2 -> colors.group3
        3 -> colors.group4
        else -> colors.group5
    }

private const val GROUP_COLOURS = 5

/** The family's shape, [size] across: a diamond, a rounded square or a circle. */
private fun DrawScope.drawPinShape(
    family: PinFamily,
    size: Float,
    color: Color,
    dashedEdge: Color?,
    outline: Float? = null,
) {
    val style = if (outline != null) Stroke(width = outline) else null
    when (family) {
        PinFamily.Hazard -> {
            val side = size * PinGeometry.HAZARD
            rotate(DIAMOND_DEGREES) { roundedSquare(side, 6.dp.toPx(), color, dashedEdge, style) }
        }

        PinFamily.Blocked -> {
            roundedSquare(size * PinGeometry.BLOCKED, 8.dp.toPx(), color, dashedEdge, style)
        }

        PinFamily.Safe -> {
            if (style != null) {
                drawCircle(color, radius = size / 2, style = style)
            } else {
                drawCircle(color, radius = size / 2)
                if (dashedEdge != null) {
                    val width = 3.dp.toPx()
                    drawCircle(dashedEdge, radius = size / 2 - width / 2, style = dashed(width))
                }
            }
        }
    }
}

private fun DrawScope.roundedSquare(
    side: Float,
    corner: Float,
    color: Color,
    dashedEdge: Color?,
    style: Stroke?,
) {
    val topLeft = Offset(center.x - side / 2, center.y - side / 2)
    val radius = CornerRadius(corner, corner)
    if (style != null) {
        drawRoundRect(color, topLeft, Size(side, side), radius, style = style)
        return
    }
    drawRoundRect(color, topLeft, Size(side, side), radius)
    if (dashedEdge != null) {
        val width = 3.dp.toPx()
        val inset = width / 2
        drawRoundRect(
            dashedEdge,
            Offset(topLeft.x + inset, topLeft.y + inset),
            Size(side - width, side - width),
            radius,
            style = dashed(width),
        )
    }
}

private fun DrawScope.dashed(width: Float) =
    Stroke(width = width, pathEffect = PathEffect.dashPathEffect(PinGeometry.dash.map { it.dp.toPx() }.toFloatArray()))

private const val DIAMOND_DEGREES = 45f
