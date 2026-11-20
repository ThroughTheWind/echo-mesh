package io.github.throughthewind.echo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.github.throughthewind.echo.core.designsystem.theme.HapticsLevel
import io.github.throughthewind.echo.core.designsystem.theme.LocalHaptics
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** How long the hold lasts (token `hold-to-confirm`). */
val HOLD_TO_CONFIRM: Duration = 2.seconds

/** A haptic tick while holding (token `hold-tick`). */
private val HOLD_TICK: Duration = 500.milliseconds

/**
 * An irreversible action that must be deliberate but fast (components/HoldToConfirmButton): hold for
 * [duration] and [onConfirmed] runs; let go at any point and nothing happens. An `error` fill grows
 * left to right, carrying a second copy of the label clipped to it so the text reads over both.
 * TalkBack gets a custom action named [accessibilityAction] instead of the hold.
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    holdingLabel: (secondsLeft: Double) -> String,
    accessibilityAction: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    duration: Duration = HOLD_TO_CONFIRM,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val confirm by rememberUpdatedState(onConfirmed)
    val haptics = LocalHapticFeedback.current
    val ticks = LocalHaptics.current != HapticsLevel.Off
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(holding) {
        while (holding && ticks) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            delay(HOLD_TICK)
        }
    }
    val container = MaterialTheme.colorScheme.errorContainer
    val onContainer = MaterialTheme.colorScheme.onErrorContainer
    val fill = MaterialTheme.colorScheme.error
    val onFill = MaterialTheme.colorScheme.onError
    val left = (1 - progress.value) * duration.inWholeMilliseconds / 1000.0
    val text = if (holding) holdingLabel(ceil(left * 10) / 10) else label
    Box(
        modifier
            .fillMaxWidth()
            .height(Touch.hero)
            .clip(CircleShape)
            .background(container)
            .semantics {
                role = Role.Button
                customActions =
                    listOf(
                        CustomAccessibilityAction(accessibilityAction) {
                            confirm()
                            true
                        },
                    )
            }.pointerInput(duration) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        val run =
                            scope.launch {
                                progress.snapTo(0f)
                                progress.animateTo(1f, tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing))
                                holding = false
                                confirm()
                            }
                        val released = tryAwaitRelease()
                        if (run.isActive || !released) {
                            run.cancel()
                            holding = false
                            scope.launch { progress.snapTo(0f) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .align(Alignment.CenterStart)
                .background(fill),
        )
        Label(text, onContainer, Modifier.fillMaxSize())
        // The same label in the fill's colour, drawn only over the filled part.
        Label(
            text,
            onFill,
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() }
                },
        )
    }
}

@Composable
private fun Label(
    text: String,
    color: Color,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}
