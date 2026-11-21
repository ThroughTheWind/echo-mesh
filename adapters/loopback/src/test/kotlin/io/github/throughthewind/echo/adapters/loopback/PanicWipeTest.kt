package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The mesh's part of the panic wipe (WI-024, FR-11.2, FR-11.4). */
class PanicWipeTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    @Test
    fun `a wipe stops the radio, sends nothing, drops what was pending and forgets every group`() =
        runTest {
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = Random(23), scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
            val (pa, pb) = List(2) { network.phone() }
            network.link(pa, pb)
            val (a, b) = listOf(pa, pb).mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(60 + i)) }
            a.turnOn()
            b.turnOn()
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            assertNotNull(b.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
            assertTrue(b.send(ReportType.MEDIC, here, group = group.id))
            delay(7.seconds)
            a.updatePosition(here)
            assertTrue(a.startSharing(group.id, until = null))
            assertTrue(a.send(ReportType.KETTLE, here))
            assertEquals(1, a.reports.value.size)
            val heldByB = b.reports.value.size

            a.wipe()
            delay(100.milliseconds)
            assertEquals(MeshState.Off, a.state.value)
            assertNull(a.pending.value, "the report in its grace period is dropped unsent")
            assertEquals(emptyList(), a.sharing.value)
            assertNull(a.sharingStopped.value, "no message about sharing: a fresh install has none")
            assertEquals(emptyList(), a.groups.value)
            assertEquals(emptyList(), a.reports.value)
            assertFalse(a.startSharing(group.id, until = null))

            delay(2.minutes)
            assertEquals(heldByB, b.reports.value.size, "nothing reached the other phone after the wipe")
            assertEquals(
                listOf("Fox", "Owl"),
                b.groups.value
                    .single()
                    .members,
                "copies on other phones stay",
            )
        }
}
