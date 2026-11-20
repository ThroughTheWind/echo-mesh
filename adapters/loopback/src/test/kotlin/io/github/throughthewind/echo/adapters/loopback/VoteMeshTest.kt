package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.mesh.report.wireMinute
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Confirm and dismiss on the mesh (WI-019): AC-7 and AC-8, and the group rule of FR-6.4. */
class VoteMeshTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    /** [count] phones all in range of each other, with nodes, all on. */
    private suspend fun TestScope.crowd(count: Int): List<MeshNode> {
        val start = testScheduler.currentTime
        val network = LoopbackNetwork(random = Random(11), scope = backgroundScope, clock = { testScheduler.currentTime - start })
        val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
        val phones = List(count) { network.phone() }
        for (i in phones.indices) for (j in i + 1 until phones.size) network.link(phones[i], phones[j])
        val nodes = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(20 + i)) }
        nodes.forEach { it.turnOn() }
        delay(1.seconds)
        return nodes
    }

    private fun MeshNode.only(): ShownReport = reports.value.single()

    @Test
    fun `AC-7 a confirmation shows on every phone and refreshes the time left, never past twice the default`() =
        runTest {
            val (a, b, c) = crowd(3)
            assertTrue(a.send(ReportType.TEAR_GAS, here))
            delay(7.seconds)
            val id = a.only().id
            val created = a.only().report.createdAt
            assertEquals(15.minutes, b.only().report.lifetime)
            assertFalse(a.vote(id, Vote.CONFIRM), "never on your own report")

            delay(10.minutes)
            assertTrue(b.vote(id, Vote.CONFIRM))
            assertFalse(b.vote(id, Vote.DISMISS), "once per report")
            delay(3.seconds)
            for (phone in listOf(a, b, c)) {
                val seen = phone.only()
                assertEquals(1, seen.tally.nearbyConfirmed)
                assertEquals(created + 25.minutes, seen.report.expiresAt, "full lifetime from the confirmation")
            }
            assertEquals(Vote.CONFIRM, b.only().myVote)

            delay(10.minutes)
            assertTrue(c.vote(id, Vote.CONFIRM))
            delay(3.seconds)
            assertEquals(created + 30.minutes, a.only().report.expiresAt, "capped at twice the default")
            assertEquals(2, a.only().tally.nearbyConfirmed)
            delay(9.minutes)
            assertEquals(created.wireMinute() + 30, a.only().tally.expiresMinute, "still held past its first expiry")
        }

    @Test
    fun `AC-8 dismissals fade a pin but never remove it`() =
        runTest {
            val nodes = crowd(5)
            val (a, b, c, d, e) = nodes
            assertTrue(a.send(ReportType.MEDIC, here))
            delay(7.seconds)
            val id = a.only().id
            assertTrue(b.vote(id, Vote.DISMISS))
            assertTrue(c.vote(id, Vote.DISMISS))
            delay(3.seconds)
            assertFalse(e.only().tally.probablyGone, "two are not enough")
            assertTrue(d.vote(id, Vote.DISMISS))
            delay(3.seconds)
            for (phone in nodes) {
                val seen = phone.only()
                assertTrue(seen.tally.probablyGone)
                assertEquals(3, seen.tally.nearbyDismissed)
            }
            assertTrue(e.vote(id, Vote.CONFIRM))
            delay(3.seconds)
            assertTrue(a.only().tally.probablyGone, "3 against 1 is still more than twice")
        }

    @Test
    fun `a group member's dismissal fades a group report until another member confirms`() =
        runTest {
            val (a, b, c) = crowd(3)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            assertNotNull(b.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
            assertNotNull(c.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Jay"))
            delay(3.seconds)
            assertTrue(a.send(ReportType.KETTLE, here, group = group.id))
            delay(7.seconds)
            val id = a.only().id
            assertTrue(b.vote(id, Vote.DISMISS))
            delay(3.seconds)
            assertTrue(a.only().tally.probablyGone)
            assertEquals(1, c.only().tally.groupDismissed)
            delay(1.minutes)
            assertTrue(c.vote(id, Vote.CONFIRM))
            delay(3.seconds)
            assertFalse(a.only().tally.probablyGone, "a later group confirmation clears it")
            assertEquals(1, b.only().tally.groupConfirmed)
        }
}
