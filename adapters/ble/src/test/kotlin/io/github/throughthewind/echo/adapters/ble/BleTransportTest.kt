package io.github.throughthewind.echo.adapters.ble

import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.TransportEvent
import io.github.throughthewind.echo.core.mesh.TransportUnavailableException
import io.github.throughthewind.echo.core.mesh.Unavailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** What the BLE transport adds to the contract: rotation, hostile links, and a radio that goes away. */
class BleTransportTest {
    private val config = BleConfig(rotateEvery = 1.minutes, rotateJitter = 0.0)

    private fun CoroutineScope.transport(
        radio: BleRadio,
        seed: Int,
    ) = BleTransport(radio, Random(seed), config, coroutineContext.minusKey(Job))

    private suspend fun BleTransport.next(): TransportEvent = withTimeout(10_000) { events.first() }

    private suspend fun BleTransport.nextSeen(): PeerHandle =
        withTimeout(10_000) { events.filterIsInstance<TransportEvent.PeerSeen>().first().peer }

    @Test
    fun `rotation drops every link and comes back with a new token, PSM and handle`() =
        runTest {
            val air = FakeAir()
            val a = transport(air.radio(), 1)
            val b = transport(air.radio(), 2)
            a.start()
            b.start()
            val first = b.nextSeen()
            val adsBefore = air.advertised.size

            assertEquals(TransportEvent.PeerLost(first), withTimeout(2.minutes) { b.events.first() })
            val second = withTimeout(2.minutes) { b.nextSeen() }
            assertNotEquals(first, second)

            val ads = air.advertised.mapNotNull { Advertisement.decode(it) }
            assertTrue(ads.size > adsBefore, "a rotation puts new advertisements on the air")
            assertEquals(ads.size, ads.map { it.token }.toSet().size, "a token is never advertised twice")
            assertEquals(ads.size, ads.map { it.psm }.toSet().size, "a PSM is never reused")
            a.stop()
            b.stop()
        }

    @Test
    fun `stopping takes the advertisement off the air`() =
        runTest {
            val air = FakeAir()
            val a = transport(air.radio(), 1)
            a.start()
            testScheduler.runCurrent()
            assertEquals(1, air.ads.value.size)
            a.stop()
            assertEquals(0, air.ads.value.size)
        }

    @Test
    fun `a bad frame length drops that link, not the phone`() =
        runTest {
            val air = FakeAir()
            val target = air.radio()
            val a = transport(target, 1)
            a.start()
            val hostile = air.hostileLinkTo(target)
            hostile.writeRaw(Advertisement.tokenBytes(7))
            val peer = a.nextSeen()
            hostile.writeRaw(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
            assertEquals(TransportEvent.PeerLost(peer), a.next())

            // Still on the mesh: an honest phone gets through afterwards.
            val b = transport(air.radio(), 2)
            b.start()
            a.nextSeen()
            a.stop()
            b.stop()
        }

    @Test
    fun `a link that never says hello is closed`() =
        runTest {
            val air = FakeAir()
            val target = air.radio()
            val a = transport(target, 1)
            a.start()
            val silent = air.hostileLinkTo(target)
            assertEquals(-1, withTimeout(config.helloTimeout + 1.seconds) { silent.read(ByteArray(1)) })
            a.stop()
        }

    @Test
    fun `a radio that is off or not allowed refuses to start, with the reason`() =
        runTest {
            val radio = FakeAir().radio()
            Unavailable.entries.forEach { reason ->
                radio.problem = reason
                assertEquals(reason, assertFailsWith<TransportUnavailableException> { transport(radio, 1).start() }.reason)
            }
        }

    @Test
    fun `a session that cannot start is retried`() =
        runTest {
            val air = FakeAir()
            val flaky = air.radio().apply { listenFailures = 1 }
            val a = transport(flaky, 1)
            val b = transport(air.radio(), 2)
            a.start()
            b.start()
            withTimeout(config.retryAfter * 3) { b.nextSeen() }
            a.stop()
            b.stop()
        }

    /** A phone that ignores the protocol: it connects straight to [target]'s advertised PSM. */
    private suspend fun FakeAir.hostileLinkTo(target: FakeBleRadio): FakeLink {
        val payload = withTimeout(10_000) { ads.first { target in it } }.getValue(target)
        val ad = Advertisement.decode(payload)!!
        return radio().connect(Sighting(payload, target), ad.psm) as FakeLink
    }
}
