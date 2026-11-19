package io.github.throughthewind.echo.adapters.ble

import io.github.throughthewind.echo.core.mesh.BandwidthClass
import io.github.throughthewind.echo.core.mesh.FrameTooLargeException
import io.github.throughthewind.echo.core.mesh.LinkFailure
import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.RangeClass
import io.github.throughthewind.echo.core.mesh.TransportCapabilities
import io.github.throughthewind.echo.core.mesh.TransportEvent
import io.github.throughthewind.echo.core.mesh.TransportUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** How the BLE transport spends the radio. First values; the field test (WI-028) tunes them. */
data class BleConfig(
    /** Token, PSM and advertising set change together at least this often (spec FR-3.4: at most 10 min). */
    val rotateEvery: Duration = 10.minutes,
    /** Rotation is drawn between this share of [rotateEvery] and all of it, so phones don't rotate in step. */
    val rotateJitter: Double = 0.2,
    /** Open links at once; past it, new phones are left to others (threat model §5.8). */
    val maxLinks: Int = 8,
    /** New outgoing connections per minute, against blind-connection floods and battery (§5.1). */
    val connectsPerMinute: Int = 12,
    /** Time for an incoming link to say which advertisement it answers. */
    val helloTimeout: Duration = 5.seconds,
    /** Wait before trying again when a session cannot start, for example with Bluetooth off. */
    val retryAfter: Duration = 5.seconds,
) {
    init {
        require(rotateEvery <= 10.minutes && rotateEvery.isPositive()) { "rotation must be at most 10 minutes" }
        require(rotateJitter in 0.0..1.0) { "jitter is a share of the period" }
        require(maxLinks > 0 && connectsPerMinute > 0) { "limits must be positive" }
    }
}

/**
 * The Bluetooth LE transport (WI-011): legacy advertising for discovery, an L2CAP channel per
 * neighbour for frames (WI-007 chose L2CAP over GATT). A thin adapter: frames in, frames out, no
 * parsing or relaying (.ai/rules/transports.md).
 *
 * Nothing stable on the air (threat model §5.1): the advertised token and PSM are random and change
 * together at every rotation, which also drops every link, so phones that met before reconnect as
 * strangers. Of two phones, the one with the lower token connects; the other waits. A [PeerHandle]
 * is random per link and never derived from an address.
 */
class BleTransport(
    private val radio: BleRadio,
    private val random: Random,
    private val config: BleConfig = BleConfig(),
    /** Where its loops run. Production uses [Dispatchers.IO]; tests pass their own. */
    private val context: CoroutineContext = Dispatchers.IO,
) : MeshTransport {
    override val capabilities: TransportCapabilities = CAPABILITIES

    private val channel = Channel<TransportEvent>(Channel.UNLIMITED)
    override val events: Flow<TransportEvent> = channel.receiveAsFlow()

    private val framing = Framing(CAPABILITIES.maxFrameBytes)
    private val lock = Mutex()
    private var scope: CoroutineScope? = null
    private var session: Session? = null
    private val links = LinkedHashMap<PeerHandle, Open>()

    /** One rotation period: what is on the air now. */
    private class Session(
        val token: Int,
        val listener: Listener,
        val jobs: MutableList<Job> = mutableListOf(),
    )

    private class Open(
        val link: Link,
        /** The token it was found by, so the same advertisement is not connected twice. */
        val remoteToken: Int,
        val writes: Mutex = Mutex(),
        var reader: Job? = null,
    )

    /** Tokens being connected to right now. */
    private val connecting = mutableSetOf<Int>()
    private val recentConnects = ArrayDeque<Long>()
    private var clockMillis: () -> Long = { System.currentTimeMillis() }

    override suspend fun start() {
        radio.unavailable()?.let { throw TransportUnavailableException(it) }
        lock.withLock {
            if (scope != null) return
            // A radio error ends one loop, never the app: whatever escapes a loop stops here, unlogged.
            val newScope = CoroutineScope(SupervisorJob() + context + CoroutineExceptionHandler { _, _ -> })
            scope = newScope
            newScope.launch { rotateLoop() }
        }
    }

    override suspend fun stop() {
        val old =
            lock.withLock {
                val s = scope ?: return
                scope = null
                s
            }
        endSession()
        old.coroutineContext[Job]?.cancelAndJoin()
    }

    override suspend fun broadcast(frame: ByteArray) {
        checkSize(frame)
        val peers = lock.withLock { links.keys.toList() }
        peers.forEach { send(it, frame) }
    }

    override suspend fun send(
        peer: PeerHandle,
        frame: ByteArray,
    ) {
        checkSize(frame)
        val open = lock.withLock { links[peer] } ?: return
        try {
            open.writes.withLock { open.link.write(framing.encode(frame)) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            drop(peer)
        }
    }

    private fun checkSize(frame: ByteArray) {
        if (frame.size > CAPABILITIES.maxFrameBytes) throw FrameTooLargeException(frame.size, CAPABILITIES.maxFrameBytes)
    }

    /**
     * Starts a session, waits out its period, then drops it and starts the next (FR-3.4). A session
     * that fails to start (Bluetooth turned off under it) is retried after [BleConfig.retryAfter].
     */
    private suspend fun rotateLoop() {
        val scope = scope ?: return
        while (scope.isActive) {
            val started =
                try {
                    beginSession(scope)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            if (!started) channel.trySend(TransportEvent.LinkFailed(LinkFailure.SESSION_START))
            if (started) {
                val share = 1.0 - config.rotateJitter * random.nextDouble()
                delay(config.rotateEvery * share)
            } else {
                delay(config.retryAfter)
            }
            endSession()
        }
    }

    private suspend fun beginSession(scope: CoroutineScope) {
        val listener = radio.listen()
        val token = random.nextInt()
        val current = Session(token, listener)
        lock.withLock { session = current }
        current.jobs += scope.launch { acceptLoop(current) }
        current.jobs += scope.launch { radio.sightings().catch { }.collect { seen(current, it) } }
        radio.advertise(Advertisement(token, listener.psm).encode())
    }

    /** Takes the advertisement off the air, closes the server and drops every link. */
    private suspend fun endSession() {
        val ending = lock.withLock { session.also { session = null } } ?: return
        runCatching { radio.stopAdvertising() }
        ending.listener.close()
        ending.jobs.forEach { it.cancel() }
        val peers = lock.withLock { links.keys.toList() }
        peers.forEach { drop(it) }
        lock.withLock { connecting.clear() }
    }

    private suspend fun acceptLoop(current: Session) {
        while (true) {
            val link =
                try {
                    current.listener.accept()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: IOException) {
                    return
                }
            scope?.launch { greet(current, link) }
        }
    }

    /** An incoming link names the advertisement it answers in its first 4 bytes. */
    private suspend fun greet(
        current: Session,
        link: Link,
    ) {
        val hello =
            try {
                withTimeoutOrNull(config.helloTimeout) {
                    val bytes = ByteArray(HELLO)
                    var read = 0
                    while (read < HELLO) {
                        val chunk = ByteArray(HELLO - read)
                        val n = link.read(chunk)
                        if (n < 0) return@withTimeoutOrNull null
                        chunk.copyInto(bytes, read, 0, n)
                        read += n
                    }
                    bytes
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                null
            }
        if (hello == null) {
            channel.trySend(TransportEvent.LinkFailed(LinkFailure.HELLO))
            link.close()
            return
        }
        if (!admit(current, Advertisement.tokenOf(hello), link)) link.close()
    }

    private suspend fun seen(
        current: Session,
        sighting: Sighting,
    ) {
        val ad = Advertisement.decode(sighting.payload) ?: return
        if (ad.token == current.token) return
        // The lower token connects, so two phones open one link between them, not two.
        if (Integer.compareUnsigned(current.token, ad.token) > 0) return
        val go =
            lock.withLock {
                val already = links.values.any { it.remoteToken == ad.token } || ad.token in connecting
                if (already || links.size >= config.maxLinks || !takeConnectBudget()) {
                    false
                } else {
                    connecting += ad.token
                    true
                }
            }
        if (!go) return
        try {
            val link = radio.connect(sighting, ad.psm)
            link.write(Advertisement.tokenBytes(current.token))
            if (!admit(current, ad.token, link)) link.close()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            // Out of range or refused: the next advertisement heard tries again.
            channel.trySend(TransportEvent.LinkFailed(LinkFailure.CONNECT))
        } catch (_: SecurityException) {
            // A permission withdrawn while on: the next session start finds it.
        } finally {
            lock.withLock { connecting -= ad.token }
        }
    }

    private fun takeConnectBudget(): Boolean {
        val now = clockMillis()
        while (recentConnects.isNotEmpty() && now - recentConnects.first() > MINUTE_MILLIS) recentConnects.removeFirst()
        if (recentConnects.size >= config.connectsPerMinute) return false
        recentConnects.addLast(now)
        return true
    }

    /** Registers [link] and starts reading it, unless the session ended or the phone is full. */
    private suspend fun admit(
        current: Session,
        remoteToken: Int,
        link: Link,
    ): Boolean {
        val scope = scope ?: return false
        val peer = PeerHandle(handleToken())
        val open = Open(link, remoteToken)
        val ok =
            lock.withLock {
                if (session !== current || links.size >= config.maxLinks || links.values.any { it.remoteToken == remoteToken }) {
                    false
                } else {
                    links[peer] = open
                    true
                }
            }
        if (!ok) return false
        channel.trySend(TransportEvent.PeerSeen(peer))
        open.reader = scope.launch { readLoop(peer, open) }
        return true
    }

    private suspend fun readLoop(
        peer: PeerHandle,
        open: Open,
    ) {
        try {
            while (true) {
                val frame = framing.read(open.link) ?: break
                channel.trySend(TransportEvent.FrameReceived(peer, frame))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            // A bad length or a broken channel: the link goes, the phone stays on the mesh.
            channel.trySend(TransportEvent.LinkFailed(LinkFailure.LINK_BROKEN))
        }
        drop(peer)
    }

    private suspend fun drop(peer: PeerHandle) {
        val open = lock.withLock { links.remove(peer) } ?: return
        open.link.close()
        open.reader?.cancel()
        channel.trySend(TransportEvent.PeerLost(peer))
    }

    private fun handleToken(): String = random.nextBytes(HANDLE_BYTES).joinToString("") { "%02x".format(it) }

    internal fun useClock(millis: () -> Long) {
        clockMillis = millis
    }

    companion object {
        /** A public report is 114 bytes and the largest frame 240 (ADR-0005); 256 leaves room. */
        val CAPABILITIES =
            TransportCapabilities(
                range = RangeClass.STREET,
                bandwidth = BandwidthClass.LOW,
                maxFrameBytes = 256,
                canBroadcast = true,
                worksInBackground = true,
                needsLocationPermission = false,
            )

        private const val HELLO = 4
        private const val HANDLE_BYTES = 8
        private const val MINUTE_MILLIS = 60_000L
    }
}
