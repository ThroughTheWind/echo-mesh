package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The screens' door to the mesh (WI-013): on and off, the 5-second grace period, withdrawal. */
class MeshNodeTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    /** Two phones in range of each other, each with a node, both off. */
    private fun TestScope.pair(): List<MeshNode> {
        val start = testScheduler.currentTime
        val network = LoopbackNetwork(random = Random(3), scope = backgroundScope, clock = { testScheduler.currentTime - start })
        val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
        val phones = List(2) { network.phone() }
        network.link(phones[0], phones[1])
        return phones.map { MeshNode(it, backgroundScope, clock, Random(4)) }
    }

    @Test
    fun `the mesh starts off, and a build with no radio says so instead of pretending`() =
        runTest {
            val (a, _) = pair()
            assertEquals(MeshState.Off, a.state.value)
            val none = MeshNode(null, backgroundScope, { epoch }, Random(5))
            assertEquals(MeshState.NoRadio, none.state.value)
            none.turnOn()
            assertEquals(MeshState.NoRadio, none.state.value)
            assertFalse(none.send(ReportType.MEDIC, here))
        }

    @Test
    fun `a report waits out its grace period, then reaches the other phone`() =
        runTest {
            val (a, b) = pair()
            a.turnOn()
            b.turnOn()
            delay(1.seconds)
            assertEquals(1, assertIs<MeshState.On>(a.state.value).nearby)
            assertTrue(a.send(ReportType.TEAR_GAS, here))
            assertFalse(a.send(ReportType.MEDIC, here), "one report waits at a time")
            delay(4.seconds)
            assertNotNull(a.pending.value)
            assertEquals(emptyList(), b.reports.value, "nothing leaves the phone during the grace period")
            delay(2.seconds)
            assertNull(a.pending.value)
            val mine = a.reports.value.single()
            assertTrue(mine.mine)
            val theirs = b.reports.value.single()
            assertEquals(ReportType.TEAR_GAS, theirs.report.type)
            assertFalse(theirs.mine)
            assertEquals(0, theirs.hops, "it arrived directly")
            assertNotNull(assertIs<MeshState.On>(b.state.value).lastReport)
            assertNotNull(a.nextAllowed(ReportType.TEAR_GAS), "one per type per minute")
        }

    @Test
    fun `a cancelled report is never signed, never sent, and does not count against the rate limit`() =
        runTest {
            val (a, b) = pair()
            a.turnOn()
            b.turnOn()
            a.send(ReportType.KETTLE, here)
            delay(3.seconds)
            a.cancelPending()
            delay(10.seconds)
            assertEquals(emptyList(), a.reports.value)
            assertEquals(emptyList(), b.reports.value)
            assertNull(a.nextAllowed(ReportType.KETTLE))
        }

    @Test
    fun `turning off stops the radio but keeps what the phone holds, and a withdrawal removes it everywhere`() =
        runTest {
            val (a, b) = pair()
            a.turnOn()
            b.turnOn()
            a.send(ReportType.STREET_CLOSED, here)
            delay(6.seconds)
            b.turnOff()
            testScheduler.runCurrent()
            assertEquals(MeshState.Off, b.state.value)
            assertEquals(1, b.reports.value.size, "held reports stay until they expire")
            b.turnOn()
            delay(1.seconds)
            val id =
                a.reports.value
                    .single()
                    .id
            assertTrue(a.withdraw(id))
            delay(1.seconds)
            assertEquals(emptyList(), a.reports.value)
            assertEquals(emptyList(), b.reports.value)
            assertFalse(a.withdraw(id), "a report is withdrawn once")
        }
}
