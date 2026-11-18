package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.MeshTransportContract
import io.github.throughthewind.echo.core.mesh.TransportEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The transport contract still holds when every frame takes time to arrive. */
class LoopbackWithLatencyTransportTest : MeshTransportContract() {
    override fun CoroutineScope.inRange(): Pair<MeshTransport, MeshTransport> {
        val network = LoopbackNetwork(random = Random(1), scope = this)
        val a = network.phone()
        val b = network.phone()
        network.link(a, b, LinkProfile(minLatency = 50.milliseconds, maxLatency = 150.milliseconds))
        return a to b
    }
}

class CrowdSimulationTest {
    /**
     * Every phone rebroadcasts each frame the first time it hears it. Not the app's relay (WI-009):
     * just enough behaviour to make a crowd spread something, so the simulator itself is tested.
     */
    private fun TestScope.floodRelay(phones: List<LoopbackTransport>) {
        for (phone in phones) {
            val seen = mutableSetOf<List<Byte>>()
            backgroundScope.launch {
                phone.events.filterIsInstance<TransportEvent.FrameReceived>().collect { event ->
                    if (seen.add(event.frame.asList())) phone.broadcast(event.frame)
                }
            }
        }
    }

    private class Run(
        val coverage: Coverage,
        val deliveries: List<Trace.Delivery>,
    )

    /** 200 phones in a 100 m square, 15 m radio range, crowded BLE links, one report from phone 0. */
    private fun crowd(seed: Int): Run {
        lateinit var run: Run
        runTest {
            val random = Random(seed)
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = random, scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val phones = Crowd.randomGeometric(network, 200, side = 100.0, range = 15.0, random = random, profile = LinkProfile.CROWDED_BLE)
            phones.forEach { it.start() }
            floodRelay(phones)
            val report = ByteArray(114) { it.toByte() }
            phones[0].broadcast(report)
            delay(60.seconds)
            run = Run(network.trace.coverage(report.asList(), phones.size), network.trace.deliveries())
        }
        return run
    }

    @Test
    fun `a seeded 200-phone crowd replays exactly`() {
        val first = crowd(seed = 7)
        val again = crowd(seed = 7)
        assertEquals(first.deliveries, again.deliveries)
        assertEquals(first.coverage.curve(60_000, 1_000), again.coverage.curve(60_000, 1_000))
        assertNotEquals(first.deliveries, crowd(seed = 8).deliveries, "another seed gives another crowd")
    }

    @Test
    fun `a crowd spreads a report over many hops with latency and loss`() {
        val run = crowd(seed = 7)
        val coverage = run.coverage
        assertEquals(setOf(0), coverage.origins)
        assertTrue(coverage.maxHop >= 5, "max hop was ${coverage.maxHop}")
        val t90 = coverage.timeToReach(0.9)
        assertTrue(t90 != null && t90 < 60_000, "90% coverage at $t90 ms")
        // Latency is real: nobody had it at once, and the curve only rises.
        assertTrue(coverage.receipts.values.all { it.atMillis >= 200 })
        val curve = coverage.curve(60_000, 500)
        assertTrue(curve.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(run.deliveries.size > 200, "loss and duplicates: more deliveries than phones")
    }

    @Test
    fun `hops and times are measured exactly on a line`() =
        runTest {
            val network = LoopbackNetwork(random = Random(3), scope = backgroundScope, clock = { testScheduler.currentTime })
            val line = Crowd.grid(network, rows = 1, columns = 5, profile = LinkProfile(minLatency = 100.milliseconds))
            line.forEach { it.start() }
            floodRelay(line)
            val frame = byteArrayOf(1, 2, 3)
            line[0].broadcast(frame)
            delay(1.seconds)
            val coverage = network.trace.coverage(frame.asList(), line.size)
            assertEquals(setOf(0), coverage.origins)
            assertEquals((1..4).associateWith { Coverage.Receipt(100L * it, it) }, coverage.receipts)
            assertEquals(1.0, coverage.fractionAt(400))
            assertEquals(0.6, coverage.fractionAt(250))
            assertEquals(400L, coverage.timeToReach(1.0))
        }

    @Test
    fun `a grid links each phone to its four neighbours`() {
        val network = LoopbackNetwork()
        val grid = Crowd.grid(network, rows = 3, columns = 4)
        assertEquals(12, network.size)
        assertEquals(2, network.neighbourCount(grid[0]))
        assertEquals(3, network.neighbourCount(grid[1]))
        assertEquals(4, network.neighbourCount(grid[5]))
    }

    @Test
    fun `a lossy link loses about its loss rate`() =
        runTest {
            val network = LoopbackNetwork(random = Random(5), scope = backgroundScope)
            val (a, b) = List(2) { network.phone() }
            network.link(a, b, LinkProfile(lossRate = 0.3))
            a.start()
            b.start()
            repeat(10_000) { a.broadcast(byteArrayOf(it.toByte())) }
            val received = network.trace.deliveries().size
            assertTrue(received in 6_700..7_300, "received $received of 10000")
        }

    @Test
    fun `a frame in flight is lost when its link breaks`() =
        runTest {
            val network = LoopbackNetwork(random = Random(5), scope = backgroundScope)
            val (a, b) = List(2) { network.phone() }
            network.link(a, b, LinkProfile(minLatency = 1.seconds))
            a.start()
            b.start()
            a.broadcast(byteArrayOf(9))
            network.unlink(a, b)
            delay(2.seconds)
            assertTrue(network.trace.deliveries().isEmpty())
        }
}
