package io.github.throughthewind.echo.core.mesh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * What every [MeshTransport] must do, whatever radio is behind it. An adapter's test class extends
 * this and provides two transports in range of each other (.ai/rules/transports.md).
 */
abstract class MeshTransportContract {
    /** Two started-or-not transports that are in range of each other. */
    protected abstract fun CoroutineScope.inRange(): Pair<MeshTransport, MeshTransport>

    private suspend fun MeshTransport.nextPeer() = withTimeout(5_000) { events.filterIsInstance<TransportEvent.PeerSeen>().first().peer }

    @Test
    fun `two phones in range see each other and exchange a frame`() =
        runTest {
            val (a, b) = inRange()
            a.start()
            b.start()
            val bSeenByA = a.nextPeer()
            b.nextPeer()

            val frame = byteArrayOf(1, 2, 3)
            a.send(bSeenByA, frame)
            val received = withTimeout(5_000) { b.events.filterIsInstance<TransportEvent.FrameReceived>().first() }
            assertContentEquals(frame, received.frame)
            a.stop()
            b.stop()
        }

    @Test
    fun `a broadcast reaches a peer in range`() =
        runTest {
            val (a, b) = inRange()
            if (!a.capabilities.canBroadcast) return@runTest
            a.start()
            b.start()
            b.nextPeer()
            a.broadcast(byteArrayOf(42))
            val received = withTimeout(5_000) { b.events.filterIsInstance<TransportEvent.FrameReceived>().first() }
            assertContentEquals(byteArrayOf(42), received.frame)
            a.stop()
            b.stop()
        }

    @Test
    fun `an oversized frame is refused before it reaches the radio`() =
        runTest {
            val (a, b) = inRange()
            a.start()
            b.start()
            val peer = a.nextPeer()
            val tooBig = ByteArray(a.capabilities.maxFrameBytes + 1)
            assertFailsWith<FrameTooLargeException> { a.send(peer, tooBig) }
            if (a.capabilities.canBroadcast) assertFailsWith<FrameTooLargeException> { a.broadcast(tooBig) }
            a.stop()
            b.stop()
        }

    @Test
    fun `a restart gives the phone a new peer handle`() =
        runTest {
            val (a, b) = inRange()
            a.start()
            b.start()
            val first = b.nextPeer()
            a.stop()
            a.start()
            val second =
                withTimeout(5_000) {
                    b.events
                        .filterIsInstance<TransportEvent.PeerSeen>()
                        .first { it.peer != first }
                        .peer
                }
            assertNotEquals(first, second)
            a.stop()
            b.stop()
        }
}
