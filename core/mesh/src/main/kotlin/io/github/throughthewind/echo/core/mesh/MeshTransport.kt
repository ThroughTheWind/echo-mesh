package io.github.throughthewind.echo.core.mesh

import kotlinx.coroutines.flow.Flow

/**
 * One way for this phone to reach nearby phones (ADR-0002). Core and UI see only this contract and
 * [TransportCapabilities]; which radio is behind it stays inside its `:adapters:*` module.
 *
 * A transport moves opaque frames. It never parses, signs or encrypts them: authentication and
 * encryption happen in core before a frame reaches it, so a transport is untrusted plumbing.
 */
interface MeshTransport {
    val capabilities: TransportCapabilities

    /**
     * Peers appearing, disappearing and frames arriving, from [start] until [stop]. There is one
     * collector, the mesh; each event is delivered to it once.
     */
    val events: Flow<TransportEvent>

    /** Starts discovering and being discoverable. Peer handles from an earlier start are void. */
    suspend fun start()

    suspend fun stop()

    /**
     * Sends [frame] to every peer in range. Only when [TransportCapabilities.canBroadcast].
     *
     * @throws FrameTooLargeException when [frame] exceeds [TransportCapabilities.maxFrameBytes].
     */
    suspend fun broadcast(frame: ByteArray)

    /**
     * Sends [frame] to one peer. A peer that went out of range is not an error: the frame is
     * dropped, as radio would drop it, and a [TransportEvent.PeerLost] follows.
     *
     * @throws FrameTooLargeException when [frame] exceeds [TransportCapabilities.maxFrameBytes].
     */
    suspend fun send(
        peer: PeerHandle,
        frame: ByteArray,
    )
}

/**
 * What a transport can do, which is all core and UI may know about it (ADR-0002). Classes rather
 * than numbers where the real figure depends on the phone and the street; transport research fills
 * in each adapter's values.
 */
data class TransportCapabilities(
    val range: RangeClass,
    val bandwidth: BandwidthClass,
    /** The largest frame [MeshTransport.send] and [MeshTransport.broadcast] accept, in bytes. */
    val maxFrameBytes: Int,
    val canBroadcast: Boolean,
    val worksInBackground: Boolean,
    val needsLocationPermission: Boolean,
) {
    init {
        require(maxFrameBytes > 0) { "maxFrameBytes must be positive, was $maxFrameBytes" }
    }
}

enum class RangeClass { ROOM, STREET, BLOCK }

enum class BandwidthClass { TINY, LOW, HIGH }

/**
 * A peer as one transport sees it, for one session. It is not an identity: it changes whenever the
 * transport restarts or the peer rotates its radio identifiers, and it is never stored or sent
 * (brief, constraint 5: no stable identifier on the air).
 */
class PeerHandle(
    val token: String,
) {
    override fun equals(other: Any?) = other is PeerHandle && other.token == token

    override fun hashCode() = token.hashCode()

    override fun toString() = "PeerHandle(${token.take(6)}…)"
}

sealed interface TransportEvent {
    data class PeerSeen(
        val peer: PeerHandle,
    ) : TransportEvent

    data class PeerLost(
        val peer: PeerHandle,
    ) : TransportEvent

    class FrameReceived(
        val from: PeerHandle,
        val frame: ByteArray,
    ) : TransportEvent

    /**
     * A link to a nearby phone failed, for field diagnostics only (FR-17.1). It names the kind of
     * failure, never the phone. A transport may never send it; the mesh does not act on it.
     */
    data class LinkFailed(
        val kind: LinkFailure,
    ) : TransportEvent
}

/** Kinds of link failure field diagnostics count (FR-17.1), the same for every radio. */
enum class LinkFailure {
    /** This phone could not start advertising and listening, for example with the radio turned off under it. */
    SESSION_START,

    /** Opening a link to a phone heard nearby failed. */
    CONNECT,

    /** A phone opened a link to this one and did not say which advertisement it answered in time. */
    HELLO,

    /** An open link broke. */
    LINK_BROKEN,

    /** The stack stopped this phone's scan for nearby phones; the transport starts it again. */
    SCAN,

    /** The stack refused this phone's advertisement; the transport tries again. */
    ADVERTISE,
}

class FrameTooLargeException(
    size: Int,
    max: Int,
) : IllegalArgumentException("frame of $size bytes exceeds the transport's $max-byte limit")

/** Why a transport cannot start, in terms the mesh status can turn into a fix (spec FR-3.5). */
enum class Unavailable {
    /** The radio is switched off; the user can switch it on. */
    RADIO_OFF,

    /** A runtime permission is missing; the user can grant it. */
    PERMISSION_MISSING,

    /** This phone cannot do what the transport needs; nothing to fix. */
    UNSUPPORTED,
}

/** Thrown by [MeshTransport.start] when the transport cannot run now, and why. */
class TransportUnavailableException(
    val reason: Unavailable,
) : IllegalStateException("transport unavailable: $reason")
