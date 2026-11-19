package io.github.throughthewind.echo.a11y

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.StreetTheme
import io.github.throughthewind.echo.core.mesh.BandwidthClass
import io.github.throughthewind.echo.core.mesh.RangeClass
import io.github.throughthewind.echo.core.mesh.TransportCapabilities
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.Report
import io.github.throughthewind.echo.core.model.ReportType
import io.github.throughthewind.echo.ui.map.MeshSheet
import io.github.throughthewind.echo.ui.map.ReportDetailSheet
import io.github.throughthewind.echo.ui.map.ReportPicker
import io.github.throughthewind.echo.ui.map.SendingBar
import io.github.throughthewind.echo.ui.protects.WhatEchoProtects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The accessibility pass (WI-031, NFR-11, NFR-12), checked on the semantics tree TalkBack reads:
 * every action on the screens that carry a report from tap to read has a spoken name and a touch
 * target of at least [MIN_TOUCH], at normal and at double font size; and a report can be dropped
 * and read through those names alone, as TalkBack does.
 *
 * Screens that need the whole app (map, settings, groups) are covered on phones with TalkBack.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application: Echo's own starts the keystore and the mesh, which these screens don't need.
@Config(sdk = [35], application = Application::class)
class AccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val now = Clock.System.now()

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = RuntimeEnvironment.getApplication().getString(id, *args)

    private fun show(
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) = compose.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            StreetTheme { content() }
        }
    }

    /** Every clickable node has a name to speak and is big enough to hit, or the test lists it. */
    private fun everyActionIsNamedAndReachable() {
        val nodes = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue("No clickable node at all: the check is blind", nodes.isNotEmpty())
        val problems = nodes.filterNot(::isPlatformDragHandle).mapNotNull(::problem)
        assertEquals(emptyList<String>(), problems)
    }

    private fun problem(node: SemanticsNode): String? {
        val name = name(node)
        val width = with(compose.density) { node.size.width.toDp() }
        val height = with(compose.density) { node.size.height.toDp() }
        return when {
            name.isBlank() -> "an action with no spoken name at ${node.boundsInRoot}"
            width < MIN_TOUCH || height < MIN_TOUCH -> "\"$name\" is $width x $height, under $MIN_TOUCH"
            else -> null
        }
    }

    private fun name(node: SemanticsNode): String =
        (
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    .orEmpty()
                    .map { it.text }
        ).joinToString(" ").trim()

    /**
     * Material's bottom-sheet drag handle: TalkBack operates it through its custom actions (expand,
     * collapse, dismiss), and every sheet here also closes with Back and with its own button.
     */
    private fun isPlatformDragHandle(node: SemanticsNode): Boolean = name(node).contains("drag handle", ignoreCase = true)

    private fun report(
        type: ReportType,
        mine: Boolean = false,
    ) = ShownReport(
        id = Bytes(ByteArray(16) { it.toByte() }),
        report = Report(type, GeoPoint(48.8566, 2.3522), now - 2.minutes, type.defaultLifetime),
        hops = 1,
        mine = mine,
    )

    @Test
    fun `a report can be dropped by name alone`() = dropsAReport(fontScale = 1f)

    @Test
    fun `a report can be dropped by name alone at double font size`() = dropsAReport(fontScale = 2f)

    private fun dropsAReport(fontScale: Float) {
        var chosen by mutableStateOf<ReportType?>(null)
        var sent: ReportType? = null
        show(fontScale) {
            ReportPicker(
                chosen = chosen,
                nextAllowed = { null },
                now = now,
                onChoose = { chosen = it },
                onSend = { sent = it },
                onClose = {},
            )
        }
        everyActionIsNamedAndReachable()
        compose.onNodeWithContentDescription(text(R.string.type_police_line)).performClick()
        compose.waitForIdle()
        everyActionIsNamedAndReachable()
        compose.onNodeWithText(text(R.string.picker_send_everyone)).performClick()
        assertEquals(ReportType.POLICE_LINE, sent)
    }

    @Test
    fun `the grace period can be cancelled by name`() {
        var cancelled = false
        show { SendingBar(ReportType.TEAR_GAS, secondsLeft = 4, discreet = false, onCancel = { cancelled = true }) }
        everyActionIsNamedAndReachable()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertTrue(cancelled)
    }

    @Test
    fun `a report reads end to end, and a public steering report says it could be a trap`() {
        show {
            ReportDetailSheet(
                shown = report(ReportType.EXIT_OPEN),
                from = GeoPoint(48.8570, 2.3530),
                now = now,
                onWithdraw = {},
                onVote = {},
                onDismiss = {},
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.type_exit_open), substring = true).assertExists()
        compose.onNodeWithText(text(R.string.detail_could_be_trap)).assertExists()
        everyActionIsNamedAndReachable()
    }

    @Test
    fun `the mesh sheet names every control`() {
        val ble = TransportCapabilities(RangeClass.STREET, BandwidthClass.LOW, 244, true, true, false)
        show {
            MeshSheet(
                state = MeshState.On(since = now - 5.minutes, nearby = 2, lastReport = now - 30.seconds, recentPeers = 2),
                radios = listOf(ble),
                simulatedRadio = false,
                now = now,
                meantOn = true,
                onToggle = {},
                onFix = {},
                onDismiss = {},
                onOpenDiagnostics = {},
            )
        }
        compose.waitForIdle()
        everyActionIsNamedAndReachable()
    }

    @Test
    fun `What Echo protects shows every line and names its actions, at double font size`() {
        show(fontScale = 2f) { WhatEchoProtects(onBack = {}) }
        for (id in listOf(R.string.protects_1, R.string.protects_6, R.string.not_protects_1, R.string.not_protects_7)) {
            compose.onNodeWithText(text(id)).assertExists()
        }
        everyActionIsNamedAndReachable()
    }

    private companion object {
        /** Android's minimum (48 dp); Echo's own buttons are 64 dp and up (foundations/Accessibility). */
        val MIN_TOUCH = 48.dp
    }
}
