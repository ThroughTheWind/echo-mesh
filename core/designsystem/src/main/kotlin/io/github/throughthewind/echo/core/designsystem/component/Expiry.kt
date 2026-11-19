package io.github.throughthewind.echo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.core.designsystem.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Time left at a glance (components/ExpiryIndicator, spec FR-7.2). Minutes only, never seconds:
 * seconds invite staring.
 */
object Expiry {
    /** Below this fraction the bar turns `blocked`; the ring keeps its colour. */
    const val ENDING = 0.1f

    /** Time left over the current lifetime, 0 to 1. */
    fun fraction(
        left: Duration,
        lifetime: Duration,
    ): Float =
        if (!lifetime.isPositive()) {
            0f
        } else {
            (left / lifetime).toFloat().coerceIn(0f, 1f)
        }

    /** Whole minutes left, rounded down, or null under one minute ("under 1 min"). */
    fun wholeMinutes(left: Duration): Int? = if (left < 1.minutes) null else left.inWholeMinutes.toInt()
}

/** "14 min left", or "under 1 min left". */
@Composable
fun timeLeftLabel(left: Duration): String =
    Expiry.wholeMinutes(left)?.let { stringResource(R.string.ds_time_left, it) }
        ?: stringResource(R.string.ds_time_left_under_one)

/** The compact form for lists and TalkBack hints: "14m". */
@Composable
fun timeLeftCompact(left: Duration): String =
    Expiry.wholeMinutes(left)?.let { stringResource(R.string.ds_time_compact, it) }
        ?: stringResource(R.string.ds_time_compact_under_one)

/** The ring on every pin: a 12 o'clock arc shrinking clockwise as time runs out. */
internal fun DrawScope.drawExpiryRing(
    fraction: Float,
    radius: Float,
    width: Float,
    color: Color,
    track: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val size = Size(radius * 2, radius * 2)
    drawCircle(track, radius = radius, style = Stroke(width))
    drawArc(
        color,
        START_ANGLE,
        FULL_TURN * fraction,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width, cap = StrokeCap.Butt),
    )
}

private const val START_ANGLE = -90f
private const val FULL_TURN = 360f

/**
 * The bar in the detail sheet: "14 min left", "of 20" on the right, an 8dp bar beneath, `blocked`
 * under 10% left. [maxLifetime] is shown when confirmations refreshed the report (FR-6.3).
 */
@Composable
fun ExpiryBar(
    left: Duration,
    lifetime: Duration,
    modifier: Modifier = Modifier,
    maxLifetime: Duration? = null,
) {
    val fraction = Expiry.fraction(left, lifetime)
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(timeLeftLabel(left), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.ds_time_of_total, lifetime.inWholeMinutes.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (fraction < Expiry.ENDING) LocalStreetColors.current.blocked else colors.onSurface,
            trackColor = colors.surfaceContainerHighest,
            drawStopIndicator = {},
        )
        if (maxLifetime != null) {
            Text(
                stringResource(R.string.ds_time_refreshed, maxLifetime.inWholeMinutes.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
