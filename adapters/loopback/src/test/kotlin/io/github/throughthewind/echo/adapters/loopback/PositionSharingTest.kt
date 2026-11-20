package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.group.GroupChannel
import io.github.throughthewind.echo.core.mesh.group.SealedRecords
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.node.SharingStopped
import io.github.throughthewind.echo.core.mesh.report.WireUnits
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.GeoPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Position sharing (WI-020): opt-in per group, short-lived, memory only (FR-10, AC-15). */
class PositionSharingTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val there = GeoPoint(48.8681, 2.3640)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    /** What the sealed files would hold, in clear, so a test can search it. */
    private class MemoryRecords : SealedRecords {
        val files = LinkedHashMap<String, ByteArray>()

        override fun names() = files.keys.toSet()

        override fun read(name: String) = files[name]?.copyOf()

        override fun write(
            name: String,
            plaintext: ByteArray,
        ) {
            files[name] = plaintext.copyOf()
        }

        override fun delete(name: String) {
            files.remove(name)
        }
    }

    private class Crew(
        val a: MeshNode,
        val b: MeshNode,
        val c: MeshNode,
        val group: Bytes,
        val kept: List<MemoryRecords>,
    )

    /** A and B in a group, C nearby and outside it, all in range and on. */
    private suspend fun TestScope.crew(): Crew {
        val start = testScheduler.currentTime
        val network = LoopbackNetwork(random = Random(13), scope = backgroundScope, clock = { testScheduler.currentTime - start })
        val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
        val phones = List(3) { network.phone() }
        for (i in phones.indices) for (j in i + 1 until phones.size) network.link(phones[i], phones[j])
        val kept = List(3) { MemoryRecords() }
        val (a, b, c) = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(30 + i), groupVault = kept[i]) }
        listOf(a, b, c).forEach { it.turnOn() }
        delay(1.seconds)
        val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
        assertNotNull(b.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
        delay(2.seconds)
        return Crew(a, b, c, group.id, kept)
    }

    @Test
    fun `AC-15 B shares, A sees it with its age, and after Stop it goes stale then disappears`() =
        runTest {
            val crew = crew()
            val (a, b, c) = Triple(crew.a, crew.b, crew.c)
            assertEquals(emptyList(), b.sharing.value, "off by default")
            b.updatePosition(here)
            assertTrue(b.startSharing(crew.group, until = null))
            delay(3.seconds)
            val seen = a.positions.value.single()
            assertEquals("Owl", seen.nickname)
            assertEquals("Crew", seen.groupName)
            assertEquals(here.latitude, seen.position.latitude, 1e-6)
            assertTrue(seen.at > epoch, "it carries when it was taken")
            assertEquals(emptyList(), b.positions.value, "your own is not a member's marker")
            assertEquals(emptyList(), c.positions.value, "only the group can read it")

            b.updatePosition(there)
            delay(SHARE.inWholeMilliseconds.milliseconds)
            assertEquals(
                there.latitude,
                a.positions.value
                    .single()
                    .position.latitude,
                1e-6,
                "only the latest per member",
            )

            b.stopSharing(crew.group)
            assertEquals(emptyList(), b.sharing.value)
            assertEquals(null, b.sharingStopped.value, "a tap on Stop needs no explanation")
            val last =
                a.positions.value
                    .single()
                    .at
            delay(3.minutes)
            assertEquals(
                last,
                a.positions.value
                    .single()
                    .at,
                "nothing new after Stop: the marker ages",
            )
            delay(GroupChannel.POSITION_LIFETIME)
            assertEquals(emptyList(), a.positions.value, "then it disappears")
        }

    @Test
    fun `sharing stops by itself when time is up, when the mesh goes off, and never starts while off`() =
        runTest {
            val crew = crew()
            val b = crew.b
            b.updatePosition(here)
            assertTrue(b.startSharing(crew.group, until = epoch + 4.minutes))
            delay(5.minutes)
            assertEquals(emptyList(), b.sharing.value)
            assertEquals(SharingStopped("Crew", SharingStopped.Why.TIME_UP), b.sharingStopped.value)
            b.sharingStopNoted()

            assertTrue(b.startSharing(crew.group, until = null))
            b.turnOff()
            assertEquals(emptyList(), b.sharing.value)
            assertEquals(SharingStopped.Why.MESH_OFF, b.sharingStopped.value?.why)
            assertFalse(b.startSharing(crew.group, until = null), "not while the mesh is off")
            assertFalse(crew.c.startSharing(crew.group, until = null), "not with a group you are not in")
        }

    @Test
    fun `a fix that is not fresh is not sent, so the group sees it go stale`() =
        runTest {
            val crew = crew()
            crew.b.updatePosition(here)
            assertTrue(crew.b.startSharing(crew.group, until = null))
            delay(3.seconds)
            val first =
                crew.a.positions.value
                    .single()
                    .at
            delay(MeshNode.FIX_FRESH + SHARE)
            assertTrue(
                crew.b.sharing.value
                    .isNotEmpty(),
                "still sharing",
            )
            val latest =
                crew.a.positions.value
                    .singleOrNull()
                    ?.at
            assertTrue(latest == null || latest - first <= MeshNode.FIX_FRESH, "nothing older than a fresh fix is re-sent")
            delay(GroupChannel.POSITION_LIFETIME)
            assertEquals(emptyList(), crew.a.positions.value)
        }

    @Test
    fun `no position reaches storage on any phone`() =
        runTest {
            val crew = crew()
            crew.b.updatePosition(here)
            assertTrue(crew.b.startSharing(crew.group, until = null))
            delay(5.minutes)
            assertTrue(
                crew.a.positions.value
                    .isNotEmpty(),
            )
            val needles =
                listOf(here.latitude, here.longitude).map { ByteBuffer.allocate(4).putInt(WireUnits.of(it)).array() }
            for (store in crew.kept) {
                assertTrue(store.files.keys.all { it.startsWith("group-") }, "only group records are kept")
                for (bytes in store.files.values) for (needle in needles) assertFalse(bytes.contains(needle), "a position in storage")
            }
        }

    private fun ByteArray.contains(needle: ByteArray): Boolean =
        (0..size - needle.size).any { i -> needle.indices.all { this[i + it] == needle[it] } }

    private companion object {
        val SHARE = MeshNode.SHARE_EVERY
    }
}
