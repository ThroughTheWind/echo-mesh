package io.github.throughthewind.protestmap.adapters.loopback

import io.github.throughthewind.protestmap.core.mesh.MeshTransport
import io.github.throughthewind.protestmap.core.mesh.MeshTransportContract
import io.github.throughthewind.protestmap.core.mesh.TransportEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class LoopbackTransportTest : MeshTransportContract() {
    override fun CoroutineScope.inRange(): Pair<MeshTransport, MeshTransport> {
        val network = LoopbackNetwork()
        val a = network.phone()
        val b = network.phone()
        network.link(a, b)
        return a to b
    }

    @Test
    fun `a frame does not hop on its own`() =
        runTest {
            // a — b — c: a's broadcast reaches b only. Relaying is core:mesh's job, not the radio's.
            val network = LoopbackNetwork()
            val (a, b, c) = List(3) { network.phone() }
            network.link(a, b)
            network.link(b, c)
            listOf(a, b, c).forEach { it.start() }
            a.broadcast(byteArrayOf(7))
            val atB = withTimeout(1_000) { b.events.filterIsInstance<TransportEvent.FrameReceived>().first() }
            assertContentEquals(byteArrayOf(7), atB.frame)
            c.broadcast(byteArrayOf(8))
            val next = withTimeout(1_000) { b.events.filterIsInstance<TransportEvent.FrameReceived>().first() }
            assertContentEquals(byteArrayOf(8), next.frame, "b got c's frame next, so a's never reached c through b")
        }
}
