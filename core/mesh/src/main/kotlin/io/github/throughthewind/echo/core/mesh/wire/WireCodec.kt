package io.github.throughthewind.echo.core.mesh.wire

/** Why a frame was dropped. Never sent back to the peer: a rejected frame is silently dropped. */
enum class Rejection {
    EMPTY,
    TOO_LARGE,
    UNKNOWN_VERSION,
    UNKNOWN_KIND,
    WRONG_LENGTH,
    HOP_BUDGET_OUT_OF_RANGE,
    UNKNOWN_REPORT_TYPE,
    POSITION_OUT_OF_RANGE,
    LIFETIME_OUT_OF_RANGE,
    UNKNOWN_VOTE,
}

sealed interface DecodeResult {
    /**
     * A frame whose layout is valid. Nothing in it is verified yet.
     *
     * [signedBytes] and [contentBytes] are slices of the bytes received, never a re-encoding: the
     * signature is checked over [signedBytes] and the message id is the hash of [contentBytes].
     */
    class Accepted(
        val frame: Frame,
        /** Everything but the hop budget: the input to the message id. */
        val contentBytes: Bytes,
        /** For a [SignedFrame], [contentBytes] without the signature; null for a group frame. */
        val signedBytes: Bytes?,
    ) : DecodeResult

    data class Rejected(
        val reason: Rejection,
    ) : DecodeResult
}

/**
 * Decodes and encodes [Wire] version 1 frames.
 *
 * The decoder reads hostile bytes. It checks the size before anything else, reads only fixed
 * offsets inside a length it has already checked, never allocates from a value read off the air,
 * and returns a [DecodeResult.Rejected] instead of throwing (.ai/rules/crypto.md). Its fuzz test is
 * `WireFuzzTest`.
 */
object WireCodec {
    fun decode(
        bytes: ByteArray,
        maxFrameBytes: Int = Wire.MAX_FRAME_BYTES,
    ): DecodeResult {
        // Size first, before a single field is read.
        if (bytes.isEmpty()) return reject(Rejection.EMPTY)
        if (bytes.size > maxFrameBytes || bytes.size > Wire.MAX_FRAME_BYTES) return reject(Rejection.TOO_LARGE)
        if (bytes.u8(0) != Wire.VERSION) return reject(Rejection.UNKNOWN_VERSION)
        if (bytes.size < Wire.HEADER_BYTES) return reject(Rejection.WRONG_LENGTH)
        val kind = FrameKind.of(bytes.u8(1)) ?: return reject(Rejection.UNKNOWN_KIND)
        if (bytes.size != kind.size) return reject(Rejection.WRONG_LENGTH)
        val hops = bytes.u8(Wire.HOP_BUDGET_OFFSET)
        if (hops > Wire.MAX_HOP_BUDGET) return reject(Rejection.HOP_BUDGET_OUT_OF_RANGE)

        val r = Reader(bytes, Wire.HEADER_BYTES)
        val frame: Frame =
            when (kind) {
                FrameKind.PUBLIC_REPORT -> {
                    val key = r.bytes(Wire.PUBLIC_KEY_BYTES)
                    val type = ReportType.of(r.u8()) ?: return reject(Rejection.UNKNOWN_REPORT_TYPE)
                    val latitude = r.i32()
                    val longitude = r.i32()
                    if (!validPosition(latitude, longitude)) return reject(Rejection.POSITION_OUT_OF_RANGE)
                    val created = r.u32()
                    val lifetime = r.u16()
                    if (!validLifetime(lifetime)) return reject(Rejection.LIFETIME_OUT_OF_RANGE)
                    PublicReportFrame(hops, key, type, latitude, longitude, created, lifetime, r.bytes(Wire.SIGNATURE_BYTES))
                }

                FrameKind.PUBLIC_VOTE -> {
                    val key = r.bytes(Wire.PUBLIC_KEY_BYTES)
                    val target = r.bytes(Wire.MESSAGE_ID_BYTES)
                    val vote = Vote.of(r.u8()) ?: return reject(Rejection.UNKNOWN_VOTE)
                    val created = r.u32()
                    val lifetime = r.u16()
                    if (!validLifetime(lifetime)) return reject(Rejection.LIFETIME_OUT_OF_RANGE)
                    PublicVoteFrame(hops, key, target, vote, created, lifetime, r.bytes(Wire.SIGNATURE_BYTES))
                }

                FrameKind.WITHDRAWAL -> {
                    val key = r.bytes(Wire.PUBLIC_KEY_BYTES)
                    val target = r.bytes(Wire.MESSAGE_ID_BYTES)
                    val created = r.u32()
                    val lifetime = r.u16()
                    if (!validLifetime(lifetime)) return reject(Rejection.LIFETIME_OUT_OF_RANGE)
                    WithdrawalFrame(hops, key, target, created, lifetime, r.bytes(Wire.SIGNATURE_BYTES))
                }

                FrameKind.GROUP -> {
                    GroupFrame(hops, r.bytes(Wire.NONCE_BYTES), r.bytes(Wire.GROUP_CIPHERTEXT_BYTES))
                }
            }
        check(r.offset == bytes.size) { "layout of $kind does not add up to its size" }

        val content = bytes.withoutHopBudget()
        val signed = if (frame is SignedFrame) Bytes(content.copyOf(content.size - Wire.SIGNATURE_BYTES)) else null
        return DecodeResult.Accepted(frame, Bytes(content), signed)
    }

    /** The bytes on the air for [frame]. One frame has exactly one encoding. */
    fun encode(frame: Frame): ByteArray {
        validate(frame)
        val out = ByteArray(frame.kind.size)
        val w = Writer(out)
        w.u8(Wire.VERSION)
        w.u8(frame.kind.code)
        w.u8(frame.hopBudget)
        when (frame) {
            is PublicReportFrame -> {
                w.bytes(frame.publicKey, Wire.PUBLIC_KEY_BYTES)
                w.u8(frame.type.code)
                w.i32(frame.latitude)
                w.i32(frame.longitude)
                w.u32(frame.createdMinute)
                w.u16(frame.lifetimeMinutes)
                w.bytes(frame.signature, Wire.SIGNATURE_BYTES)
            }

            is PublicVoteFrame -> {
                w.bytes(frame.publicKey, Wire.PUBLIC_KEY_BYTES)
                w.bytes(frame.target, Wire.MESSAGE_ID_BYTES)
                w.u8(frame.vote.code)
                w.u32(frame.createdMinute)
                w.u16(frame.lifetimeMinutes)
                w.bytes(frame.signature, Wire.SIGNATURE_BYTES)
            }

            is WithdrawalFrame -> {
                w.bytes(frame.publicKey, Wire.PUBLIC_KEY_BYTES)
                w.bytes(frame.target, Wire.MESSAGE_ID_BYTES)
                w.u32(frame.createdMinute)
                w.u16(frame.lifetimeMinutes)
                w.bytes(frame.signature, Wire.SIGNATURE_BYTES)
            }

            is GroupFrame -> {
                w.bytes(frame.nonce, Wire.NONCE_BYTES)
                w.bytes(frame.ciphertext, Wire.GROUP_CIPHERTEXT_BYTES)
            }
        }
        check(w.offset == out.size) { "layout of ${frame.kind} does not add up to its size" }
        return out
    }

    /**
     * What the author signs for [frame]: its encoding without the hop budget and the signature. The
     * signature field's content is ignored, so a sender can pass a frame with a blank signature.
     */
    fun signingInput(frame: SignedFrame): ByteArray {
        val content = encode(frame).withoutHopBudget()
        return content.copyOf(content.size - Wire.SIGNATURE_BYTES)
    }

    private fun validate(frame: Frame) {
        require(frame.hopBudget in 0..Wire.MAX_HOP_BUDGET) { "hop budget out of range: ${frame.hopBudget}" }
        when (frame) {
            is PublicReportFrame -> {
                require(validPosition(frame.latitude, frame.longitude)) { "position out of range" }
                require(validLifetime(frame.lifetimeMinutes)) { "lifetime out of range: ${frame.lifetimeMinutes}" }
                require(frame.createdMinute in 0..U32_MAX) { "creation minute out of range" }
            }

            is PublicVoteFrame -> {
                require(validLifetime(frame.lifetimeMinutes)) { "lifetime out of range: ${frame.lifetimeMinutes}" }
                require(frame.createdMinute in 0..U32_MAX) { "creation minute out of range" }
            }

            is WithdrawalFrame -> {
                require(validLifetime(frame.lifetimeMinutes)) { "lifetime out of range: ${frame.lifetimeMinutes}" }
                require(frame.createdMinute in 0..U32_MAX) { "creation minute out of range" }
            }

            is GroupFrame -> {}
        }
    }

    private fun validPosition(
        latitude: Int,
        longitude: Int,
    ) = latitude in -90 * Wire.DEGREE_UNITS..90 * Wire.DEGREE_UNITS &&
        longitude in -180 * Wire.DEGREE_UNITS..180 * Wire.DEGREE_UNITS

    private fun validLifetime(minutes: Int) = minutes in 1..Wire.MAX_LIFETIME_MINUTES

    private fun reject(reason: Rejection) = DecodeResult.Rejected(reason)

    private fun ByteArray.u8(at: Int) = this[at].toInt() and 0xFF

    private fun ByteArray.withoutHopBudget(): ByteArray {
        val out = ByteArray(size - 1)
        copyInto(out, 0, 0, Wire.HOP_BUDGET_OFFSET)
        copyInto(out, Wire.HOP_BUDGET_OFFSET, Wire.HOP_BUDGET_OFFSET + 1, size)
        return out
    }

    private const val U32_MAX = 0xFFFF_FFFFL

    /** Reads big-endian fields at fixed offsets of a buffer whose length was checked beforehand. */
    private class Reader(
        private val buf: ByteArray,
        var offset: Int,
    ) {
        fun u8(): Int = buf[offset++].toInt() and 0xFF

        fun u16(): Int = (u8() shl 8) or u8()

        fun i32(): Int = (u8() shl 24) or (u8() shl 16) or (u8() shl 8) or u8()

        fun u32(): Long = i32().toLong() and U32_MAX

        /** [n] is always a constant of [Wire], never a value read from the frame. */
        fun bytes(n: Int): Bytes = Bytes(buf.copyOfRange(offset, offset + n)).also { offset += n }
    }

    private class Writer(
        private val buf: ByteArray,
    ) {
        var offset = 0

        fun u8(v: Int) {
            buf[offset++] = v.toByte()
        }

        fun u16(v: Int) {
            u8(v ushr 8)
            u8(v)
        }

        fun i32(v: Int) {
            u8(v ushr 24)
            u8(v ushr 16)
            u8(v ushr 8)
            u8(v)
        }

        fun u32(v: Long) = i32(v.toInt())

        fun bytes(
            value: Bytes,
            n: Int,
        ) {
            require(value.size == n) { "field must be $n bytes, was ${value.size}" }
            value.writeTo(buf, offset)
            offset += n
        }
    }
}
