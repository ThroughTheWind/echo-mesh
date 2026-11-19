package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.crypto.Hashes
import io.github.throughthewind.echo.core.crypto.Signatures
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.Rejection
import io.github.throughthewind.echo.core.mesh.wire.SignedFrame
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame

/** Why a frame from the air was dropped. Never sent back: a rejected frame is dropped silently. */
enum class FrameRejection {
    /** The layout or a value range is wrong; [Validation.Rejected.wire] says which. */
    MALFORMED,

    /** Created more than [FrameValidator.FUTURE_TOLERANCE_MINUTES] after this phone's now. */
    FROM_THE_FUTURE,

    /** Past its signed expiry, plus [FrameValidator.EXPIRY_GRACE_MINUTES] of tolerated skew. */
    EXPIRED,

    /** Its message id is one this phone already holds. */
    DUPLICATE,

    /** The signature does not verify over the bytes received with the key in the frame. */
    BAD_SIGNATURE,

    /** A kind this version of the app does not act on yet (group frames: WI-018), or a summary. */
    UNSUPPORTED_KIND,

    /** The link that sent it has used up its signature checks for now (threat model §5.8). */
    OVER_BUDGET,
}

sealed interface Validation {
    /**
     * A frame that may be stored and relayed. Its signature verified over the exact bytes received;
     * [id] is the SHA-256 of those bytes without the hop budget (FR-5.2), so it names the content,
     * never the sender or a relay.
     */
    class Valid(
        val id: Bytes,
        val frame: SignedFrame,
        /** The frame as it arrived, for a relay to forward with a lower hop budget. */
        val received: Bytes,
    ) : Validation

    data class Rejected(
        val reason: FrameRejection,
        /** The decoder's reason, for [FrameRejection.MALFORMED] only. */
        val wire: Rejection? = null,
        /** The frame's signed creation minute, once decoded: what a clock check reads (FR-7.3). */
        val createdMinute: Long? = null,
    ) : Validation
}

/**
 * Checks a frame from the air in the order of threat model §5.4: size and version (the decoder),
 * expiry, de-duplication, signature, then meaning. The cheap checks come first, so a flood of junk
 * costs a phone as little as possible, and nothing is acted on before its signature verifies.
 *
 * One pure function from bytes to a [Validation]; the relay (WI-009) calls it on every frame.
 */
object FrameValidator {
    /** A frame created up to this many minutes in this phone's future is still accepted (§5.2). */
    const val FUTURE_TOLERANCE_MINUTES: Long = 5

    /** A frame is dropped this many minutes after its signed expiry, for clock skew (§5.2). */
    const val EXPIRY_GRACE_MINUTES: Long = 2

    /**
     * Validates [bytes] at [nowMinute] (minutes since the Unix epoch, UTC). [seen] answers whether a
     * message id is already held; it runs after expiry and before the signature, as §5.4 orders.
     * [mayVerify] is asked just before the signature check, so a link's verification budget is spent
     * only on frames that got that far (threat model §5.8).
     */
    fun validate(
        bytes: ByteArray,
        nowMinute: Long,
        maxFrameBytes: Int = Wire.MAX_FRAME_BYTES,
        seen: (Bytes) -> Boolean = { false },
        mayVerify: () -> Boolean = { true },
    ): Validation {
        val decoded =
            when (val result = WireCodec.decode(bytes, maxFrameBytes)) {
                is DecodeResult.Rejected -> return Validation.Rejected(FrameRejection.MALFORMED, wire = result.reason)
                is DecodeResult.Accepted -> result
            }
        val frame = decoded.frame as? SignedFrame ?: return Validation.Rejected(FrameRejection.UNSUPPORTED_KIND)
        val created = frame.createdMinute
        if (created > nowMinute + FUTURE_TOLERANCE_MINUTES) {
            return Validation.Rejected(FrameRejection.FROM_THE_FUTURE, createdMinute = created)
        }
        if (nowMinute >= created + frame.lifetimeMinutes + EXPIRY_GRACE_MINUTES) {
            return Validation.Rejected(FrameRejection.EXPIRED, createdMinute = created)
        }
        val id = Bytes(Hashes.sha256(decoded.contentBytes.toByteArray()))
        if (seen(id)) return Validation.Rejected(FrameRejection.DUPLICATE, createdMinute = created)
        if (!mayVerify()) return Validation.Rejected(FrameRejection.OVER_BUDGET, createdMinute = created)
        val signed = checkNotNull(decoded.signedBytes) { "a signed frame always has signed bytes" }
        if (!Signatures.verify(frame.publicKey.toByteArray(), frame.signature.toByteArray(), signed.toByteArray())) {
            return Validation.Rejected(FrameRejection.BAD_SIGNATURE, createdMinute = created)
        }
        return Validation.Valid(id, frame, Bytes(bytes))
    }

    /**
     * True when [withdrawal] may remove [report]: it names the report's id and is signed by the same
     * one-time key (FR-4.5, threat model §5.2). Both must already be [Validation.Valid].
     */
    fun withdraws(
        withdrawal: WithdrawalFrame,
        report: PublicReportFrame,
        reportId: Bytes,
    ): Boolean = withdrawal.target == reportId && withdrawal.publicKey == report.publicKey
}
