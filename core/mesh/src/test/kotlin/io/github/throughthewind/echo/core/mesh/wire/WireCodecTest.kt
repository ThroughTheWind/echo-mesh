package io.github.throughthewind.echo.core.mesh.wire

import io.github.throughthewind.echo.core.model.ReportType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class WireCodecTest {
    private fun rejected(bytes: ByteArray) = assertIs<DecodeResult.Rejected>(WireCodec.decode(bytes)).reason

    private fun accepted(bytes: ByteArray) = assertIs<DecodeResult.Accepted>(WireCodec.decode(bytes))

    private fun ByteArray.with(
        at: Int,
        value: Int,
    ) = copyOf().also { it[at] = value.toByte() }

    @ParameterizedTest
    @MethodSource("frames")
    fun `every frame kind round-trips to the same bytes`(frame: Frame) {
        val bytes = WireCodec.encode(frame)
        assertEquals(frame.kind.size, bytes.size)
        assertEquals(frame, accepted(bytes).frame)
        WireProperty.check(bytes)
    }

    @Test
    fun `frame sizes fit the smallest transport and match the research estimate`() {
        assertEquals(114, FrameKind.PUBLIC_REPORT.size)
        assertEquals(138, FrameKind.PUBLIC_VOTE.size)
        assertEquals(137, FrameKind.WITHDRAWAL.size)
        assertEquals(240, FrameKind.GROUP.size)
        assert(FrameKind.entries.all { it.size <= Wire.MAX_FRAME_BYTES })
        // The loopback radio's default, modelled on one BLE L2CAP packet, carries every kind.
        assert(Wire.MAX_FRAME_BYTES <= 244)
    }

    @ParameterizedTest
    @MethodSource("frames")
    fun `a truncated frame is rejected at every length`(frame: Frame) {
        val bytes = WireCodec.encode(frame)
        assertEquals(Rejection.EMPTY, rejected(ByteArray(0)))
        for (n in 1 until bytes.size) {
            assertEquals(Rejection.WRONG_LENGTH, rejected(bytes.copyOf(n)), "truncated to $n bytes")
        }
    }

    @ParameterizedTest
    @MethodSource("frames")
    fun `a trailing byte is rejected`(frame: Frame) {
        val bytes = WireCodec.encode(frame) + byteArrayOf(0)
        val reason = rejected(bytes)
        assert(reason == Rejection.WRONG_LENGTH || reason == Rejection.TOO_LARGE) { "was $reason" }
    }

    @Test
    fun `an oversized frame is rejected before its version is read`() {
        // Version byte valid, kind valid: only the size is wrong.
        val big = ByteArray(Wire.MAX_FRAME_BYTES + 1).with(0, Wire.VERSION).with(1, FrameKind.GROUP.code)
        assertEquals(Rejection.TOO_LARGE, rejected(big))
        assertEquals(Rejection.TOO_LARGE, rejected(ByteArray(1_000_000)))
        // A transport with a smaller limit passes it in.
        val report = WireCodec.encode(WireFixtures.report)
        assertEquals(Rejection.TOO_LARGE, (WireCodec.decode(report, maxFrameBytes = 100) as DecodeResult.Rejected).reason)
    }

    @ParameterizedTest
    @MethodSource("frames")
    fun `an unknown version is rejected`(frame: Frame) {
        val bytes = WireCodec.encode(frame)
        for (v in listOf(0, 2, 0x7F, 0xFF)) assertEquals(Rejection.UNKNOWN_VERSION, rejected(bytes.with(0, v)))
    }

    @Test
    fun `an unknown kind is rejected`() {
        val bytes = WireCodec.encode(WireFixtures.report)
        for (k in listOf(0, 5, 0xFF)) assertEquals(Rejection.UNKNOWN_KIND, rejected(bytes.with(1, k)))
    }

    @Test
    fun `a hop budget above the ceiling is rejected`() {
        val bytes = WireCodec.encode(WireFixtures.report)
        assertEquals(Rejection.HOP_BUDGET_OUT_OF_RANGE, rejected(bytes.with(Wire.HOP_BUDGET_OFFSET, Wire.MAX_HOP_BUDGET + 1)))
        assertEquals(Rejection.HOP_BUDGET_OUT_OF_RANGE, rejected(bytes.with(Wire.HOP_BUDGET_OFFSET, 0xFF)))
        assertFailsWith<IllegalArgumentException> { WireCodec.encode(WireFixtures.report.copy(hopBudget = 8)) }
    }

    @Test
    fun `fields outside their range are rejected`() {
        val report = WireCodec.encode(WireFixtures.report)
        val typeAt = Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES
        assertEquals(Rejection.UNKNOWN_REPORT_TYPE, rejected(report.with(typeAt, ReportType.entries.size)))
        // Latitude's top byte set: far beyond 90 degrees.
        assertEquals(Rejection.POSITION_OUT_OF_RANGE, rejected(report.with(typeAt + 1, 0x7F)))
        val lifetimeAt = typeAt + 1 + 4 + 4 + 4
        assertEquals(Rejection.LIFETIME_OUT_OF_RANGE, rejected(report.with(lifetimeAt, 0).with(lifetimeAt + 1, 0)))
        assertEquals(Rejection.LIFETIME_OUT_OF_RANGE, rejected(report.with(lifetimeAt + 1, Wire.MAX_LIFETIME_MINUTES + 1)))

        val vote = WireCodec.encode(WireFixtures.vote)
        val voteAt = Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES + Wire.MESSAGE_ID_BYTES
        for (v in listOf(0, 3, 0xFF)) assertEquals(Rejection.UNKNOWN_VOTE, rejected(vote.with(voteAt, v)))
    }

    @Test
    fun `the hop budget is outside the signed bytes and the message id`() {
        val a = accepted(WireCodec.encode(WireFixtures.report))
        val b = accepted(WireCodec.encode(WireFixtures.report.withHopBudget(1)))
        assertEquals(a.contentBytes, b.contentBytes)
        assertEquals(a.signedBytes, b.signedBytes)
        // Every other byte is covered: flipping any of them changes the signed bytes or the signature.
        val bytes = WireCodec.encode(WireFixtures.report)
        val signatureAt = bytes.size - Wire.SIGNATURE_BYTES
        for (i in bytes.indices) {
            if (i == Wire.HOP_BUDGET_OFFSET) continue
            val flipped = WireCodec.decode(bytes.with(i, bytes[i].toInt() xor 1))
            if (flipped !is DecodeResult.Accepted) continue
            if (i < signatureAt) {
                assertNotEquals(a.signedBytes, flipped.signedBytes, "byte $i is not signed")
            } else {
                assertNotEquals((a.frame as SignedFrame).signature, (flipped.frame as SignedFrame).signature)
            }
        }
    }

    @Test
    fun `signed bytes are the bytes received, not a re-encoding`() {
        val bytes = WireCodec.encode(WireFixtures.vote)
        val result = accepted(bytes)
        val expected = bytes.copyOfRange(0, 2) + bytes.copyOfRange(Wire.HEADER_BYTES, bytes.size - Wire.SIGNATURE_BYTES)
        assertContentEquals(expected, result.signedBytes!!.toByteArray())
    }

    @Test
    fun `random bytes never make the decoder throw`() {
        val random = Random(20261117)
        repeat(20_000) {
            val size = random.nextInt(0, Wire.MAX_FRAME_BYTES + 3)
            val bytes = random.nextBytes(size)
            // Bias toward a valid header, so the fields behind it are reached.
            if (size > 1 && random.nextBoolean()) {
                bytes[0] = Wire.VERSION.toByte()
                bytes[1] =
                    FrameKind.entries
                        .random(random)
                        .code
                        .toByte()
            }
            WireProperty.check(bytes)
        }
    }

    @Test
    fun `bytes never print`() {
        assertEquals("Bytes(32)", WireFixtures.report.publicKey.toString())
    }

    companion object {
        @JvmStatic
        fun frames() = WireFixtures.all
    }
}
