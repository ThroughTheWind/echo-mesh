package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.group.GroupChannel
import io.github.throughthewind.echo.core.mesh.group.MemberInfo
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The roster shows members seen recently and their age, nothing older (FR-8.7, WI-022). */
class RosterSeenTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    private fun MeshNode.member(nickname: String): MemberInfo =
        groups.value
            .single()
            .roster
            .single { it.nickname == nickname }

    @Test
    fun `a member is seen when they join or send, and drops off after 15 minutes of silence`() =
        runTest {
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = Random(19), scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
            val phones = List(3) { network.phone() }
            for (i in phones.indices) for (j in i + 1 until phones.size) network.link(phones[i], phones[j])
            val (a, b, c) = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(50 + i)) }
            listOf(a, b, c).forEach { it.turnOn() }
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            assertNotNull(b.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
            delay(2.seconds)
            assertNotNull(a.member("Owl").lastSeen, "seen joining in front of this phone")
            assertNotNull(c.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Jay"))
            delay(2.seconds)
            assertNotNull(b.member("Jay").lastSeen, "seen through the member-added")

            delay(10.minutes)
            assertTrue(c.send(ReportType.MEDIC, here, group = group.id))
            delay(7.seconds)
            delay(6.minutes)
            assertNull(a.member("Owl").lastSeen, "silent for over 15 minutes: no age shown")
            assertNotNull(a.member("Jay").lastSeen, "Jay sent a report 6 minutes ago")
            delay(GroupChannel.SEEN_FOR)
            assertNull(a.member("Jay").lastSeen)
        }
}
