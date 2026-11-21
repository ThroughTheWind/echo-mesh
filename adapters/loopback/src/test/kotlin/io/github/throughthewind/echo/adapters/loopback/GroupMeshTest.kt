package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.group.JoinEvent
import io.github.throughthewind.echo.core.mesh.group.JoinOffer
import io.github.throughthewind.echo.core.mesh.group.SealedRecords
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Groups on the mesh (WI-017, WI-018): joining in person by QR code over one hop, then group
 * reports that cross a phone outside the group, which relays them without reading them.
 */
class GroupMeshTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    private class Mesh(
        val network: LoopbackNetwork,
        val phones: List<LoopbackTransport>,
        val nodes: List<MeshNode>,
    )

    /** [count] phones with nodes, none linked yet, all on. */
    private suspend fun TestScope.mesh(count: Int): Mesh {
        val start = testScheduler.currentTime
        val network = LoopbackNetwork(random = Random(7), scope = backgroundScope, clock = { testScheduler.currentTime - start })
        val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
        val phones = List(count) { network.phone() }
        val nodes = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(10 + i)) }
        nodes.forEach { it.turnOn() }
        return Mesh(network, phones, nodes)
    }

    @Test
    fun `a newcomer joins by QR code, and the code works once`() =
        runTest {
            val mesh = mesh(3)
            val (a, c, d) = mesh.nodes
            mesh.network.link(mesh.phones[0], mesh.phones[1])
            mesh.network.link(mesh.phones[0], mesh.phones[2])
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            val shown = mutableListOf<JoinEvent>()
            backgroundScope.launch { a.groupChannel!!.joins.collect { shown += it } }
            val offer = assertNotNull(a.groupChannel!!.invite(group.id))
            val code = offer.encode()
            assertTrue(code.startsWith("echo-join:1:"))
            assertEquals(offer, JoinOffer.decode(code))

            val joined = assertNotNull(c.groupChannel!!.join(JoinOffer.decode(code)!!, "Owl"))
            assertEquals("Crew", joined.name)
            assertEquals(listOf(JoinEvent(group.id, "Crew", "Owl", throughThisPhone = true)), shown, "the code's phone sees who joined")
            assertEquals(group.id, joined.id)
            assertTrue(c.groupChannel!!.rename(group.id, "Friends"), "a joiner may rename the group on their phone")
            assertEquals(
                "Friends",
                c.groups.value
                    .single()
                    .name,
            )
            assertEquals(listOf("Fox", "Owl"), joined.members)
            delay(1.seconds)
            assertEquals(
                listOf("Fox", "Owl"),
                a.groups.value
                    .single()
                    .members,
                "the member who showed the code knows the newcomer",
            )

            assertNull(d.groupChannel!!.join(offer, "Jay"), "a second scan of the same code fails (AC-25)")
            assertEquals(emptyList(), d.groups.value)
            assertEquals(
                listOf("Fox", "Owl"),
                a.groups.value
                    .single()
                    .members,
            )
        }

    @Test
    fun `a wrong or tampered code lets nobody in and gets no answer`() =
        runTest {
            val mesh = mesh(2)
            val (a, b) = mesh.nodes
            mesh.network.link(mesh.phones[0], mesh.phones[1])
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            val offer = assertNotNull(a.groupChannel!!.invite(group.id))
            val token = offer.token.toByteArray().also { it[0] = (it[0] + 1).toByte() }
            assertNull(b.groupChannel!!.join(JoinOffer(offer.responderKey, Bytes(token)), "Owl"))
            assertNull(JoinOffer.decode(offer.encode() + "A"))
            assertNull(JoinOffer.decode(offer.encode().replace("echo-join:1:", "echo-join:2:")))
            // The real code still works: a stranger's attempt does not burn it.
            assertNotNull(b.groupChannel!!.join(offer, "Owl"))
        }

    @Test
    fun `a phone that replays a join it overheard lets nobody else in`() =
        runTest {
            val mesh = mesh(2)
            val (a, c) = mesh.nodes
            val hostile = HostilePhone(mesh.network, mesh.network.phone(), Random(5))
            mesh.network.link(mesh.phones[0], mesh.phones[1])
            mesh.network.link(mesh.phones[0], hostile.transport)
            mesh.network.link(mesh.phones[1], hostile.transport)
            hostile.transport.start()
            hostile.listen(backgroundScope)
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            assertNotNull(c.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
            // A fresh code is open while the replays arrive: an old step 1 must not open it either.
            assertNotNull(a.groupChannel!!.invite(group.id))
            hostile.attack(this, HostilePhone.Attack.REPLAYED, 20, 5.seconds).join()
            delay(1.seconds)
            assertEquals(
                listOf("Fox", "Owl"),
                a.groups.value
                    .single()
                    .members,
            )
            assertEquals(
                listOf("Fox", "Owl"),
                c.groups.value
                    .single()
                    .members,
            )
        }

    @Test
    fun `a group report crosses a phone outside the group, which relays it unread, and is withdrawn`() =
        runTest {
            val mesh = mesh(4)
            val (a, b, c, d) = mesh.nodes
            val (pa, pb, pc, pd) = mesh.phones
            // C joins A in person, then they walk apart with B, not in the group, between them.
            mesh.network.link(pa, pc)
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox"))
            assertNotNull(c.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
            mesh.network.unlink(pa, pc)
            mesh.network.link(pa, pb)
            mesh.network.link(pb, pc)
            delay(1.seconds)

            assertTrue(c.send(ReportType.MEDIC, here, group = group.id))
            delay(7.seconds)
            val seen = a.reports.value.single()
            assertEquals(ReportType.MEDIC, seen.report.type)
            assertEquals("Owl", seen.group?.sender)
            assertEquals("Crew", seen.group?.groupName)
            assertFalse(seen.mine)
            assertEquals(1, seen.hops, "it crossed B")
            assertEquals(emptyList(), b.reports.value, "B relays group frames it cannot read")
            assertTrue(
                c.reports.value
                    .single()
                    .mine,
            )

            // D joins through A while C is out of range; C learns of D through B (FR-8.2).
            val toldC = mutableListOf<JoinEvent>()
            backgroundScope.launch { c.groupChannel!!.joins.collect { toldC += it } }
            mesh.network.link(pa, pd)
            delay(1.seconds)
            val late = async { d.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Jay") }
            assertNotNull(late.await())
            delay(3.seconds)
            assertEquals(
                listOf("Fox", "Owl", "Jay"),
                c.groups.value
                    .single()
                    .members,
            )
            assertEquals(listOf(JoinEvent(group.id, "Crew", "Jay", throughThisPhone = false)), toldC, "every member sees the join")
            assertTrue(d.send(ReportType.KETTLE, here, group = group.id))
            delay(8.seconds)
            assertEquals(
                "Jay",
                c.reports.value
                    .single { it.report.type == ReportType.KETTLE }
                    .group
                    ?.sender,
            )
            assertEquals(emptyList(), b.reports.value)

            val id =
                c.reports.value
                    .single { it.mine }
                    .id
            assertTrue(c.withdraw(id))
            delay(3.seconds)
            assertTrue(a.reports.value.none { it.id == id }, "withdrawn everywhere the group reads it")
            assertTrue(d.reports.value.none { it.id == id })
            assertFalse(a.withdraw(id), "only its sender can withdraw it")
        }

    /** A store that forgets nothing and seals nothing: what the sealed files would hold, in clear. */
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

    @Test
    fun `groups outlive a restart, and leaving or expiry deletes what was kept`() =
        runTest {
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = Random(9), scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
            val (pa, pc, restarted) = List(3) { network.phone() }
            val kept = MemoryRecords()
            val a = MeshNode(pa, backgroundScope, clock, Random(1))
            // The first run of the app lives in its own scope, so the test can end it.
            val firstRun = CoroutineScope(backgroundScope.coroutineContext + Job())
            val c = MeshNode(pc, firstRun, clock, Random(2), groupVault = kept)
            a.turnOn()
            c.turnOn()
            network.link(pa, pc)
            delay(1.seconds)
            val group = assertNotNull(a.groupChannel!!.create("Crew", "Fox", lifetime = 4.hours))
            assertNotNull(c.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Owl"))
            assertNotNull(c.groupChannel!!.create("Solo", "Owl"))
            assertEquals(2, kept.files.size, "each group is kept as soon as it exists")

            // The phone restarts: a new node over the same store, the old one gone.
            c.turnOff()
            firstRun.cancel()
            network.unlink(pa, pc)
            val back = MeshNode(restarted, backgroundScope, clock, Random(3), groupVault = kept)
            back.turnOn()
            network.link(pa, restarted)
            delay(1.seconds)
            val crew = back.groups.value.single { it.name == "Crew" }
            assertEquals(listOf("Fox", "Owl"), crew.members)
            assertEquals("Owl", crew.myNickname)
            assertTrue(a.send(ReportType.MEDIC, here, group = group.id))
            delay(7.seconds)
            assertEquals(
                "Fox",
                back.reports.value
                    .single()
                    .group
                    ?.sender,
                "the kept keys still open the group's frames",
            )

            // Two hours on, the keys have ratcheted and the record was rewritten without the old ones.
            delay(2.hours)
            assertEquals(2, kept.files.size)
            back.groupChannel!!.leave(
                back.groups.value
                    .single { it.name == "Solo" }
                    .id,
            )
            assertEquals(1, kept.files.size, "leaving deletes the record (FR-8.5)")
            delay(2.hours)
            assertEquals(emptyList(), back.groups.value)
            assertEquals(emptyMap(), kept.files, "an ended group's record is deleted, not hidden (FR-12.4)")
        }
}
