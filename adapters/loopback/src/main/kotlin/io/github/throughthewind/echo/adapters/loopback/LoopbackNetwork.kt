package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.BandwidthClass
import io.github.throughthewind.echo.core.mesh.FrameTooLargeException
import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.RangeClass
import io.github.throughthewind.echo.core.mesh.TransportCapabilities
import io.github.throughthewind.echo.core.mesh.TransportEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How one radio link behaves: each frame takes a latency drawn uniformly from [minLatency] to
 * [maxLatency] and is lost with probability [lossRate]. Latency needs a [LoopbackNetwork.scope].
 */
data class LinkProfile(
    val minLatency: Duration = Duration.ZERO,
    val maxLatency: Duration = minLatency,
    val lossRate: Double = 0.0,
) {
    init {
        require(!minLatency.isNegative() && maxLatency >= minLatency) { "latency range is $minLatency..$maxLatency" }
        require(lossRate in 0.0..1.0) { "loss rate must be a probability, was $lossRate" }
    }

    companion object {
        /** Instant and lossless: the default, and what hand-written tests expect. */
        val PERFECT = LinkProfile()

        /** A rough BLE connection in a crowd: a few hundred milliseconds a hop, one frame in ten lost. */
        val CROWDED_BLE = LinkProfile(minLatency = 200.milliseconds, maxLatency = 800.milliseconds, lossRate = 0.1)
    }
}

/**
 * An in-memory radio for tests: many simulated phones on the JVM, in a crowd you lay out by hand or
 * generate with [Crowd]. Mesh, gossip and hostile-peer tests run on it in milliseconds (ADR-0002,
 * spec FR-14.3).
 *
 * Without a [scope], delivery is instant and lossless whatever the link says. With one, every frame
 * is delivered after its link's latency, and a run under `kotlinx-coroutines-test` virtual time is
 * fast and, from the same [random] seed, identical every time.
 *
 * The default capabilities are deliberately tight (a small frame, like one BLE packet) so code that
 * works here does not assume a fast link.
 */
class LoopbackNetwork(
    private val capabilities: TransportCapabilities = DEFAULT_CAPABILITIES,
    private val random: Random = Random.Default,
    /** Runs delayed deliveries. Null delivers at once. */
    val scope: CoroutineScope? = null,
    /** Milliseconds since the start of the run, for [trace]. Pass `{ testScheduler.currentTime }`. */
    private val clock: () -> Long = { 0L },
) {
    private val lock = Any()
    private val phones = mutableListOf<LoopbackTransport>()
    private val links = mutableMapOf<Set<LoopbackTransport>, LinkProfile>()

    /** Every frame that reached a phone, in order: the raw material of [Coverage]. */
    val trace = Trace()

    /** A new simulated phone, linked to nobody. */
    fun phone(): LoopbackTransport = synchronized(lock) { LoopbackTransport(this, capabilities, phones.size).also(phones::add) }

    val size: Int get() = synchronized(lock) { phones.size }

    /** Puts [a] and [b] in range of each other. */
    fun link(
        a: LoopbackTransport,
        b: LoopbackTransport,
        profile: LinkProfile = LinkProfile.PERFECT,
    ) = synchronized(lock) {
        require(a !== b) { "a phone is not in range of itself" }
        val key = setOf(a, b)
        val isNew = key !in links
        links[key] = profile
        if (isNew) {
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
        if (links.remove(setOf(a, b)) != null) {
            val (ha, hb) = a.handle to b.handle
            if (ha != null && hb != null) {
                a.emit(TransportEvent.PeerLost(hb))
                b.emit(TransportEvent.PeerLost(ha))
            }
        }
    }

    /** Puts every phone created so far in range of every other. */
    fun linkAll(profile: LinkProfile = LinkProfile.PERFECT) =
        synchronized(lock) {
            for (i in phones.indices) for (j in i + 1 until phones.size) link(phones[i], phones[j], profile)
        }

    fun neighbourCount(of: LoopbackTransport): Int = synchronized(lock) { neighbours(of).size }

    private fun neighbours(of: LoopbackTransport) =
        links.keys
            .filter { of in it }
            .map { pair -> pair.first { it !== of } }
            .sortedBy { it.index } // Deterministic order, so a seeded run replays exactly.

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

    /**
     * Sends [frame] from [from] to [to], or to every neighbour when [to] is null. Does not check the
     * size: [LoopbackTransport] does that for honest phones, and [HostilePhone] skips it on purpose.
     */
    internal fun deliver(
        from: LoopbackTransport,
        to: PeerHandle?,
        frame: ByteArray,
    ) = synchronized(lock) {
        val sender = from.handle ?: return@synchronized
        for (target in neighbours(from)) {
            if (target.handle == null || (to != null && target.handle != to)) continue
            val profile = links.getValue(setOf(from, target))
            if (profile.lossRate > 0.0 && random.nextDouble() < profile.lossRate) continue
            val copy = frame.copyOf()
            val latency = latency(profile)
            val scope = scope
            if (scope == null || latency == Duration.ZERO) {
                arrive(from, target, sender, copy)
            } else {
                scope.launch {
                    delay(latency)
                    synchronized(lock) { arrive(from, target, sender, copy) }
                }
            }
        }
    }

    private fun latency(profile: LinkProfile): Duration {
        val spread = (profile.maxLatency - profile.minLatency).inWholeMilliseconds
        val extra = if (spread > 0) random.nextLong(spread + 1) else 0
        return profile.minLatency + extra.milliseconds
    }

    private fun arrive(
        from: LoopbackTransport,
        to: LoopbackTransport,
        sender: PeerHandle,
        frame: ByteArray,
    ) {
        // A phone that stopped, or a link that broke, while the frame was in flight loses it.
        if (to.handle == null || setOf(from, to) !in links) return
        trace.record(clock(), from.index, to.index, frame)
        to.emit(TransportEvent.FrameReceived(sender, frame))
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
    /** Position in creation order, for traces and metrics only. Never on the air. */
    val index: Int,
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

    override fun toString() = "phone #$index"
}
