package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.MeshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The low-density state (WI-035, FR-16.1, AC-27's automated half): crossing the threshold both ways. */
class LowDensityTest {
    private val epoch = Instant.parse("2026-11-19T16:00:00Z")

    @Test
    fun `fewer than 3 phones heard in 5 minutes is low density, and the count follows the crowd both ways`() =
        runTest {
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = Random(8), scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
            val phones = List(4) { network.phone() }
            val nodes = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(70 + i)) }
            nodes.forEach { it.turnOn() }
            val a = nodes[0]

            fun on() = assertIs<MeshState.On>(a.state.value)

            delay(1.seconds)
            assertFalse(on().lowDensity(clock()), "still looking: not yet low density")

            network.link(phones[0], phones[1])
            delay(MeshNode.SEARCHING)
            assertEquals(1, on().recentPeers)
            assertTrue(on().lowDensity(clock()), "one phone heard")

            network.link(phones[0], phones[2])
            network.link(phones[0], phones[3])
            delay(2.seconds)
            assertEquals(MeshNode.LOW_DENSITY_PEERS, on().recentPeers)
            assertFalse(on().lowDensity(clock()), "three phones heard")

            listOf(1, 2, 3).forEach { network.unlink(phones[0], phones[it]) }
            delay(1.minutes)
            assertEquals(0, on().nearby, "nobody in range now")
            assertFalse(on().lowDensity(clock()), "still heard within 5 minutes")
            delay(5.minutes)
            assertEquals(0, on().recentPeers, "handles are forgotten after the window")
            assertTrue(on().lowDensity(clock()))
        }
}
