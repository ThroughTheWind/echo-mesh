package io.github.throughthewind.echo.core.mesh.wire

import io.github.throughthewind.echo.core.model.ReportType

/**
 * Wire format version 1 (ADR-0005, `docs/decisions/ADR-0005-wire-format.md`), with the summary
 * kind of ADR-0008 and the join kind of ADR-0009.
 *
 * Every frame is `version ‖ kind ‖ hop budget ‖ body`, with a fixed length per kind, no optional
 * field, no nesting, no length prefix read off the air and no compression. The hop budget is the
 * only byte a relay changes, so it is outside the signature and the message id; everything else is
 * covered by them.
 */
object Wire {
    const val VERSION: Int = 1

    /** Version, kind and hop budget. */
    const val HEADER_BYTES: Int = 3
    const val HOP_BUDGET_OFFSET: Int = 2

    const val PUBLIC_KEY_BYTES: Int = 32
    const val SIGNATURE_BYTES: Int = 64
    const val MESSAGE_ID_BYTES: Int = 32

    /** The most hops a frame may still travel. Meshtastic's ceiling; the relay picks the default. */
    const val MAX_HOP_BUDGET: Int = 7

    /** The longest life a report or vote can claim: 90 minutes of default life, doubled by votes. */
    const val MAX_LIFETIME_MINUTES: Int = 180

    /** Positions travel in units of 10⁻⁴ degree, about 11 m: coarse by construction (FR-4.4). */
    const val DEGREE_UNITS: Int = 10_000

    const val NONCE_BYTES: Int = 24
    const val TAG_BYTES: Int = 16

    /** Every group frame has exactly this size, so its content is not visible from its length. */
    const val GROUP_FRAME_BYTES: Int = 240
    const val GROUP_CIPHERTEXT_BYTES: Int = GROUP_FRAME_BYTES - HEADER_BYTES - NONCE_BYTES

    /** Plaintext room inside a group frame, once the AEAD tag is counted. */
    const val GROUP_PLAINTEXT_BYTES: Int = GROUP_CIPHERTEXT_BYTES - TAG_BYTES

    /** A summary's per-frame random salt, so two summaries of one set share no bits (ADR-0008). */
    const val SUMMARY_SALT_BYTES: Int = 16

    /** A summary's Bloom filter: the rest of a [GROUP_FRAME_BYTES] frame. */
    const val SUMMARY_FILTER_BYTES: Int = GROUP_FRAME_BYTES - HEADER_BYTES - SUMMARY_SALT_BYTES

    /** A join's session tag: random per join, so two phones tell their messages apart (ADR-0009). */
    const val JOIN_SESSION_BYTES: Int = 8

    /** The Noise message in a join frame: the rest of a [GROUP_FRAME_BYTES] frame. */
    const val JOIN_MESSAGE_BYTES: Int = GROUP_FRAME_BYTES - HEADER_BYTES - JOIN_SESSION_BYTES - 1

    /** The largest frame of any kind. A transport must carry at least this many bytes. */
    const val MAX_FRAME_BYTES: Int = GROUP_FRAME_BYTES
}

/** What a frame is. The code is the second byte on the air; an unknown code is rejected. */
enum class FrameKind(
    val code: Int,
    /** The exact length of a frame of this kind, header included. */
    val size: Int,
) {
    PUBLIC_REPORT(0x01, Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES + 1 + 4 + 4 + 4 + 2 + Wire.SIGNATURE_BYTES),
    PUBLIC_VOTE(0x02, Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES + Wire.MESSAGE_ID_BYTES + 1 + 4 + 2 + Wire.SIGNATURE_BYTES),
    WITHDRAWAL(0x03, Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES + Wire.MESSAGE_ID_BYTES + 4 + 2 + Wire.SIGNATURE_BYTES),
    GROUP(0x04, Wire.GROUP_FRAME_BYTES),

    /** What a phone holds, as a salted Bloom filter; never relayed (ADR-0008). */
    SUMMARY(0x05, Wire.HEADER_BYTES + Wire.SUMMARY_SALT_BYTES + Wire.SUMMARY_FILTER_BYTES),

    /** One step of joining a group in person, between two phones in direct range; never relayed (ADR-0009). */
    JOIN(0x06, Wire.HEADER_BYTES + Wire.JOIN_SESSION_BYTES + 1 + Wire.JOIN_MESSAGE_BYTES),
    ;

    companion object {
        fun of(code: Int): FrameKind? = entries.firstOrNull { it.code == code }
    }
}

enum class Vote(
    val code: Int,
) {
    CONFIRM(1),
    DISMISS(2),
    ;

    companion object {
        fun of(code: Int): Vote? = entries.firstOrNull { it.code == code }
    }
}

/** An immutable byte string with content equality, so frames can be compared and hashed. */
class Bytes(
    bytes: ByteArray,
) {
    private val value = bytes.copyOf()

    val size: Int get() = value.size

    fun toByteArray(): ByteArray = value.copyOf()

    internal fun writeTo(
        out: ByteArray,
        offset: Int,
    ) {
        value.copyInto(out, offset)
    }

    override fun equals(other: Any?) = other is Bytes && other.value.contentEquals(value)

    override fun hashCode() = value.contentHashCode()

    /** Never prints content: a key or a signature does not belong in a log (crypto rule). */
    override fun toString() = "Bytes($size)"
}

/** A decoded frame. Fields hold what was on the air; whether it is true is for the layers above. */
sealed interface Frame {
    val kind: FrameKind

    /** Hops the frame may still travel. Unsigned: relays decrement it. */
    val hopBudget: Int

    /** The same frame with another hop budget, for a relay to forward. */
    fun withHopBudget(hops: Int): Frame
}

/** A frame whose author signed it with a key carried in the frame (threat model §5.2). */
sealed interface SignedFrame : Frame {
    val publicKey: Bytes
    val signature: Bytes

    /** Signed creation time, minutes since the Unix epoch, UTC (spec FR-7.3). */
    val createdMinute: Long

    /** Signed lifetime in minutes: the frame is void at [createdMinute] plus this (FR-5.2). */
    val lifetimeMinutes: Int
}

/**
 * A public report. [createdMinute] is minutes since the Unix epoch, UTC. Position is in
 * 1/[Wire.DEGREE_UNITS] degree.
 */
data class PublicReportFrame(
    override val hopBudget: Int,
    override val publicKey: Bytes,
    val type: ReportType,
    val latitude: Int,
    val longitude: Int,
    override val createdMinute: Long,
    override val lifetimeMinutes: Int,
    override val signature: Bytes,
) : SignedFrame {
    override val kind get() = FrameKind.PUBLIC_REPORT

    override fun withHopBudget(hops: Int) = copy(hopBudget = hops)
}

data class PublicVoteFrame(
    override val hopBudget: Int,
    override val publicKey: Bytes,
    /** The message id of the report voted on. */
    val target: Bytes,
    val vote: Vote,
    override val createdMinute: Long,
    override val lifetimeMinutes: Int,
    override val signature: Bytes,
) : SignedFrame {
    override val kind get() = FrameKind.PUBLIC_VOTE

    override fun withHopBudget(hops: Int) = copy(hopBudget = hops)
}

/** Withdraws a report; signed by the same one-time key that signed the report (FR-4.5). */
data class WithdrawalFrame(
    override val hopBudget: Int,
    override val publicKey: Bytes,
    val target: Bytes,
    override val createdMinute: Long,
    override val lifetimeMinutes: Int,
    override val signature: Bytes,
) : SignedFrame {
    override val kind get() = FrameKind.WITHDRAWAL

    override fun withHopBudget(hops: Int) = copy(hopBudget = hops)
}

/** An encrypted group frame: opaque to everyone outside the group, and the same size as every other. */
data class GroupFrame(
    override val hopBudget: Int,
    val nonce: Bytes,
    /** Ciphertext and AEAD tag, [Wire.GROUP_CIPHERTEXT_BYTES] long. */
    val ciphertext: Bytes,
) : Frame {
    override val kind get() = FrameKind.GROUP

    override fun withHopBudget(hops: Int) = copy(hopBudget = hops)
}

/**
 * The set of message ids a phone holds, sent to a neighbour so it can push only what is missing
 * (spec FR-5.1, ADR-0008). Not signed, not stored, never relayed: its hop budget is always 0.
 */
data class SummaryFrame(
    val salt: Bytes,
    /** A Bloom filter of [Wire.SUMMARY_FILTER_BYTES] bytes over the salted ids. */
    val filter: Bytes,
) : Frame {
    override val kind get() = FrameKind.SUMMARY
    override val hopBudget: Int get() = 0

    /** A summary is never relayed, so it has no other hop budget. */
    override fun withHopBudget(hops: Int): SummaryFrame {
        require(hops == 0) { "a summary is never relayed" }
        return this
    }
}

/**
 * One step of a join (ADR-0009): a Noise handshake or transport message, padded to one size. Not
 * signed, not stored, never relayed: its hop budget is always 0.
 */
data class JoinFrame(
    /** Random per join, picked by the joiner. */
    val session: Bytes,
    /** 1 and 2 are the handshake; 3 on carry the roster. */
    val step: Int,
    /** [Wire.JOIN_MESSAGE_BYTES] bytes, opaque to everyone but the two phones joining. */
    val message: Bytes,
) : Frame {
    override val kind get() = FrameKind.JOIN
    override val hopBudget: Int get() = 0

    /** A join is never relayed, so it has no other hop budget. */
    override fun withHopBudget(hops: Int): JoinFrame {
        require(hops == 0) { "a join is never relayed" }
        return this
    }
}
