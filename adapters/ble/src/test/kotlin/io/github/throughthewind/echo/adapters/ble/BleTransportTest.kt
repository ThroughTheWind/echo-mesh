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
    fun `a phone that hears nothing still gets a link, from the other phone, without waiting for a rotation`() =
        runTest {
            // Both orders, so the deaf phone is the lower token once and the higher once.
            for (deafFirst in listOf(true, false)) {
                val air = FakeAir()
                val slow = BleConfig(rotateEvery = 10.minutes, rotateJitter = 0.0)
                val one = air.radio().also { it.deaf = deafFirst }
                val two = air.radio().also { it.deaf = !deafFirst }
                val a = BleTransport(one, Random(1), slow, coroutineContext.minusKey(Job))
                val b = BleTransport(two, Random(2), slow, coroutineContext.minusKey(Job))
                a.start()
                b.start()
                withTimeout(slow.fallbackAfter + 5.seconds) {
                    a.events.filterIsInstance<TransportEvent.PeerSeen>().first()
                    b.events.filterIsInstance<TransportEvent.PeerSeen>().first()
                }
                a.stop()
                b.stop()
            }
        }

    @Test
    fun `a scan the stack stops is started again`() =
        runTest {
            val air = FakeAir()
            // The fallback is out of the way, so only a restarted scan can make the link.
            val patient = BleConfig(rotateEvery = 10.minutes, rotateJitter = 0.0, fallbackAfter = 9.minutes)
            val one = air.radio().also { it.scanFailures = 1 }
            val two = air.radio().also { it.scanFailures = 1 }
            val a = BleTransport(one, Random(1), patient, coroutineContext.minusKey(Job))
            val b = BleTransport(two, Random(2), patient, coroutineContext.minusKey(Job))
            a.start()
            b.start()
            withTimeout(patient.retryAfter + 5.seconds) { a.events.filterIsInstance<TransportEvent.PeerSeen>().first() }
            a.stop()
            b.stop()
        }

    @Test
    fun `a phone the stack will not let advertise still connects out, and keeps trying to advertise`() =
        runTest {
            for (refusedFirst in listOf(true, false)) {
                val air = FakeAir()
                val slow = BleConfig(rotateEvery = 10.minutes, rotateJitter = 0.0)
                val one = air.radio().also { if (refusedFirst) it.advertiseFailures = 1_000 }
                val two = air.radio().also { if (!refusedFirst) it.advertiseFailures = 1_000 }
                val a = BleTransport(one, Random(1), slow, coroutineContext.minusKey(Job))
                val b = BleTransport(two, Random(2), slow, coroutineContext.minusKey(Job))
                a.start()
                b.start()
                withTimeout(slow.fallbackAfter + 5.seconds) {
                    a.events.filterIsInstance<TransportEvent.PeerSeen>().first()
                    b.events.filterIsInstance<TransportEvent.PeerSeen>().first()
                }
                a.stop()
                b.stop()
            }
        }

    @Test
    fun `a radio that keeps failing is retried less and less often`() =
        runTest {
            val air = FakeAir()
            val radio = air.radio().also { it.scanFailures = 1_000 }
            val a = BleTransport(radio, Random(1), BleConfig(rotateEvery = 10.minutes, rotateJitter = 0.0), coroutineContext.minusKey(Job))
            a.start()
            testScheduler.advanceTimeBy(30_000)
            testScheduler.runCurrent()
            // Android blocks an app that starts more than five scans in 30 s.
            assertTrue(radio.scansStarted <= 5, "${radio.scansStarted} scans started in 30 s")
            testScheduler.advanceTimeBy(10 * 60_000)
            testScheduler.runCurrent()
            assertTrue(radio.scansStarted <= 12, "${radio.scansStarted} scans started in 10 min")
            a.stop()
        }

    @Test
    fun `a server that dies under a session starts a new one, not at the next rotation`() =
        runTest {
            val air = FakeAir()
            val slow = BleConfig(rotateEvery = 10.minutes, rotateJitter = 0.0)
            val a = BleTransport(air.radio(), Random(1), slow, coroutineContext.minusKey(Job))
            a.start()
            testScheduler.runCurrent()
            val before = air.advertised.size
            air.dropListeners()
            testScheduler.runCurrent()
            testScheduler.advanceTimeBy((slow.retryAfter + 1.seconds).inWholeMilliseconds)
            testScheduler.runCurrent()
            assertTrue(air.advertised.size > before, "a new session is on the air within seconds")
            a.stop()
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
