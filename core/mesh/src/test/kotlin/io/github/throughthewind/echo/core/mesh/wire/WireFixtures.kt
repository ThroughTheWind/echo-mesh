package io.github.throughthewind.echo.core.mesh.wire

import io.github.throughthewind.echo.core.model.ReportType
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

/** Sample frames of every kind, with recognisable but meaningless keys and signatures. */
object WireFixtures {
    fun filled(
        n: Int,
        b: Int,
    ) = Bytes(ByteArray(n) { (b + it).toByte() })

    val report =
        PublicReportFrame(
            hopBudget = 5,
            publicKey = filled(Wire.PUBLIC_KEY_BYTES, 1),
            type = ReportType.TEAR_GAS,
            // Place de la République, Paris, in 10⁻⁴ degree.
            latitude = 488_675,
            longitude = 23_632,
            createdMinute = 29_870_000,
            lifetimeMinutes = 15,
            signature = filled(Wire.SIGNATURE_BYTES, 100),
        )

    val vote =
        PublicVoteFrame(
            hopBudget = 7,
            publicKey = filled(Wire.PUBLIC_KEY_BYTES, 2),
            target = filled(Wire.MESSAGE_ID_BYTES, 50),
            vote = Vote.DISMISS,
            createdMinute = 29_870_003,
            lifetimeMinutes = 12,
            signature = filled(Wire.SIGNATURE_BYTES, 101),
        )

    val withdrawal =
        WithdrawalFrame(
            hopBudget = 0,
            publicKey = filled(Wire.PUBLIC_KEY_BYTES, 1),
            target = filled(Wire.MESSAGE_ID_BYTES, 50),
            createdMinute = 29_870_004,
            lifetimeMinutes = 11,
            signature = filled(Wire.SIGNATURE_BYTES, 102),
        )

    val group =
        GroupFrame(
            hopBudget = 3,
            nonce = filled(Wire.NONCE_BYTES, 7),
            ciphertext = filled(Wire.GROUP_CIPHERTEXT_BYTES, 9),
        )

    val summary =
        SummaryFrame(
            salt = filled(Wire.SUMMARY_SALT_BYTES, 30),
            filter = filled(Wire.SUMMARY_FILTER_BYTES, 60),
        )

    val join =
        JoinFrame(
            session = filled(Wire.JOIN_SESSION_BYTES, 70),
            step = 2,
            message = filled(Wire.JOIN_MESSAGE_BYTES, 80),
        )

    val all: List<Frame> = listOf(report, vote, withdrawal, group, summary, join)
}

/**
 * The decoder's contract on any input, checked by the unit tests and on every fuzzer input: it never
 * throws, and what it accepts has exactly one encoding, equal to the bytes received.
 */
object WireProperty {
    fun check(
        input: ByteArray,
        decode: (ByteArray) -> DecodeResult = { WireCodec.decode(it) },
    ) {
        val result = decode(input)
        if (result !is DecodeResult.Accepted) return
        assertContentEquals(input, WireCodec.encode(result.frame), "an accepted frame re-encodes to other bytes")
        val content = result.contentBytes.toByteArray()
        assertEquals(input.size - 1, content.size)
        assertEquals(input[0], content[0])
        assertEquals(input[1], content[1])
        assertContentEquals(input.copyOfRange(Wire.HEADER_BYTES, input.size), content.copyOfRange(2, content.size))
        when (val frame = result.frame) {
            is SignedFrame -> {
                val signed = result.signedBytes ?: fail("a signed frame came without its signed bytes")
                assertContentEquals(WireCodec.signingInput(frame), signed.toByteArray())
                assertContentEquals(content.copyOf(content.size - Wire.SIGNATURE_BYTES), signed.toByteArray())
            }

            is GroupFrame -> {
                assertEquals(null, result.signedBytes)
                assertEquals(Wire.GROUP_FRAME_BYTES, input.size)
            }

            is SummaryFrame -> {
                assertEquals(null, result.signedBytes)
                assertEquals(0, frame.hopBudget)
            }

            is JoinFrame -> {
                assertEquals(null, result.signedBytes)
                assertEquals(0, frame.hopBudget)
                assertEquals(Wire.GROUP_FRAME_BYTES, input.size)
            }
        }
        assertIs<DecodeResult.Accepted>(decode(WireCodec.encode(result.frame.withHopBudget(0))))
    }
}
