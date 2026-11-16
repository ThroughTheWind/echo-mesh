package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.BandwidthClass
import io.github.throughthewind.echo.core.mesh.FrameTooLargeException
import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.RangeClass
import io.github.throughthewind.echo.core.mesh.TransportCapabilities
import io.github.throughthewind.echo.core.mesh.TransportEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.random.Random

/**
 * An in-memory radio for tests: many simulated phones on the JVM, with links you place by hand to
 * model a crowd. Mesh, gossip and hostile-peer tests run on it in milliseconds (ADR-0002).
 *
 * The default capabilities are deliberately tight (a small frame, like a BLE advertisement) so code
 * that works here does not assume a fast link.
 */
class LoopbackNetwork(
    private val capabilities: TransportCapabilities = DEFAULT_CAPABILITIES,
    private val random: Random = Random.Default,
) {
    private val lock = Any()
    private val phones = mutableListOf<LoopbackTransport>()
    private val links = mutableSetOf<Set<LoopbackTransport>>()

    /** A new simulated phone, linked to nobody. */
    fun phone(): LoopbackTransport = synchronized(lock) { LoopbackTransport(this, capabilities).also(phones::add) }

    /** Puts [a] and [b] in range of each other. */
    fun link(
        a: LoopbackTransport,
        b: LoopbackTransport,
    ) = synchronized(lock) {
        require(a !== b) { "a phone is not in range of itself" }
        if (links.add(setOf(a, b))) {
            val (ha, hb) = a.handle to b.handle
            if (ha != null && hb != null) {
                a.emit(TransportEvent.PeerSeen(hb))
                b.emit(TransportEvent.PeerSeen(ha))
            }
        }
    }

    /** Takes [a] and [b] out of range of each other. */
    fun unlink(
        a: LoopbackTransport,
        b: LoopbackTransport,
    ) = synchronized(lock) {
        if (links.remove(setOf(a, b))) {
            val (ha, hb) = a.handle to b.handle
            if (ha != null && hb != null) {
                a.emit(TransportEvent.PeerLost(hb))
                b.emit(TransportEvent.PeerLost(ha))
            }
        }
    }

    /** Puts every phone created so far in range of every other. */
    fun linkAll() =
        synchronized(lock) {
            for (i in phones.indices) for (j in i + 1 until phones.size) link(phones[i], phones[j])
        }

    private fun neighbours(of: LoopbackTransport) = links.filter { of in it }.map { pair -> pair.first { it !== of } }

    internal fun started(phone: LoopbackTransport) =
        synchronized(lock) {
            // A fresh random token per session: nothing about a phone survives a restart.
            val handle = PeerHandle(random.nextBytes(16).joinToString("") { "%02x".format(it) })
            phone.handle = handle
            for (other in neighbours(phone)) {
                val otherHandle = other.handle ?: continue
                other.emit(TransportEvent.PeerSeen(handle))
                phone.emit(TransportEvent.PeerSeen(otherHandle))
            }
        }

    internal fun stopped(phone: LoopbackTransport) =
        synchronized(lock) {
            val handle = phone.handle ?: return@synchronized
            phone.handle = null
            for (other in neighbours(phone)) if (other.handle != null) other.emit(TransportEvent.PeerLost(handle))
        }

    internal fun deliver(
        from: LoopbackTransport,
        to: PeerHandle?,
        frame: ByteArray,
    ) = synchronized(lock) {
        val sender = from.handle ?: return@synchronized
        neighbours(from)
            .filter { it.handle != null && (to == null || it.handle == to) }
            .forEach { it.emit(TransportEvent.FrameReceived(sender, frame.copyOf())) }
    }

    companion object {
        val DEFAULT_CAPABILITIES =
            TransportCapabilities(
                range = RangeClass.STREET,
                bandwidth = BandwidthClass.TINY,
                maxFrameBytes = 244,
                canBroadcast = true,
                worksInBackground = true,
                needsLocationPermission = false,
            )
    }
}

/** One simulated phone's radio. Create it with [LoopbackNetwork.phone]. */
class LoopbackTransport internal constructor(
    private val network: LoopbackNetwork,
    override val capabilities: TransportCapabilities,
) : MeshTransport {
    private val channel = Channel<TransportEvent>(Channel.UNLIMITED)

    override val events: Flow<TransportEvent> = channel.receiveAsFlow()

    internal var handle: PeerHandle? = null

    internal fun emit(event: TransportEvent) {
        channel.trySend(event)
    }

    override suspend fun start() {
        if (handle == null) network.started(this)
    }

    override suspend fun stop() = network.stopped(this)

    override suspend fun broadcast(frame: ByteArray) {
        checkSize(frame)
        network.deliver(this, null, frame)
    }

    override suspend fun send(
        peer: PeerHandle,
        frame: ByteArray,
    ) {
        checkSize(frame)
        network.deliver(this, peer, frame)
    }

    private fun checkSize(frame: ByteArray) {
        if (frame.size > capabilities.maxFrameBytes) throw FrameTooLargeException(frame.size, capabilities.maxFrameBytes)
    }
}
