package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Mute a member on this phone only (WI-021, FR-9.4, AC-24). */
class MuteTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    @Test
    fun `AC-24 muting hides a member's reports, votes and position here only, and unmuting brings them back`() =
        runTest {
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = Random(17), scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
            val phones = List(3) { network.phone() }
            for (i in phones.indices) for (j in i + 1 until phones.size) network.link(phones[i], phones[j])
            val (a, b, c) = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(40 + i)) }
            listOf(a, b, c).forEach { it.turnOn() }
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            assertNotNull(b.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Sage"))
            assertNotNull(c.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Jay"))
            delay(3.seconds)

            // Sage reports, dismisses Jay's report and shares a position.
            assertTrue(c.send(ReportType.MEDIC, here, group = group.id))
            delay(7.seconds)
            val jays =
                a.reports.value
                    .single()
                    .id
            assertTrue(b.send(ReportType.KETTLE, here, group = group.id))
            delay(7.seconds)
            assertTrue(b.vote(jays, Vote.DISMISS))
            b.updatePosition(here)
            assertTrue(b.startSharing(group.id, until = null))
            delay(3.seconds)
            assertEquals(2, a.reports.value.size)
            assertTrue(
                a.reports.value
                    .single { it.id == jays }
                    .tally.probablyGone,
            )
            assertEquals(
                "Sage",
                a.positions.value
                    .single()
                    .nickname,
            )

            val sage =
                a.groups.value
                    .single()
                    .roster
                    .single { it.nickname == "Sage" }
            val me =
                a.groups.value
                    .single()
                    .roster
                    .single { it.isMe }
            assertFalse(a.groupChannel!!.mute(group.id, me.id, true), "you cannot mute yourself")
            assertTrue(a.groupChannel!!.mute(group.id, sage.id, true))
            // Within one refresh: the screens' state is recombined on the node's scope, with no radio involved.
            delay(100.milliseconds)
            val shown = a.reports.value
            assertEquals(listOf(jays), shown.map { it.id }, "Sage's report is hidden")
            assertFalse(shown.single().tally.probablyGone, "Sage's dismissal no longer counts")
            assertEquals(0, shown.single().tally.groupDismissed)
            assertEquals(emptyList(), a.positions.value, "Sage's position is hidden")
            assertTrue(
                a.groups.value
                    .single()
                    .roster
                    .single { it.nickname == "Sage" }
                    .muted,
            )

            assertTrue(b.send(ReportType.TEAR_GAS, here, group = group.id))
            delay(7.seconds)
            assertEquals(1, a.reports.value.size, "a new report from Sage stays hidden")
            assertEquals(3, c.reports.value.size, "on this phone only: Jay still sees Sage")
            assertEquals(1, c.reports.value.count { it.mine })

            assertTrue(a.groupChannel!!.mute(group.id, sage.id, false))
            delay(100.milliseconds)
            assertEquals(3, a.reports.value.size, "unmuting brings them back")
            assertTrue(
                a.reports.value
                    .single { it.id == jays }
                    .tally.probablyGone,
            )
            assertEquals(
                "Sage",
                a.positions.value
                    .single()
                    .nickname,
            )
        }
}
