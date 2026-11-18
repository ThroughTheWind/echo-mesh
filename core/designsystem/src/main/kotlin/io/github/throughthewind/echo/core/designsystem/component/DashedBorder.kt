package io.github.throughthewind.echo.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The dashed edge that means "anyone could have sent this" (components/ReportPin): on public pins,
 * and on the provenance box of a public report (patterns/ReportDetail).
 */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 2.dp,
    corner: Dp = 12.dp,
): Modifier =
    drawBehind {
        val stroke = width.toPx()
        val inset = stroke / 2
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }
