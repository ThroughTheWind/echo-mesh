package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.mesh.report.ReportAuthor.Outcome
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.Rejection
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Spec AC-10 for public reports: tampered, replayed-after-expiry and forged frames are refused. */
class FrameValidatorTest {
    private var now = Instant.parse("2026-11-18T10:00:30Z")
    private val author = ReportAuthor { now }
    private val here = GeoPoint(48.8675, 2.3632)

    private fun sent(type: ReportType = ReportType.TEAR_GAS): Outcome.Sent =
        assertIs<Outcome.Sent>(author.report(type, here, hopBudget = 5))

    private fun validate(
        bytes: ByteArray,
        at: Instant = now,
        seen: (Bytes) -> Boolean = { false },
    ) = FrameValidator.validate(bytes, at.wireMinute(), seen = seen)

    private fun rejection(validation: Validation) = assertIs<Validation.Rejected>(validation).reason

    @Test
    fun `an honest report is valid, with the author's id, and says what was sent`() {
        val sent = sent()
        val valid = assertIs<Validation.Valid>(validate(sent.frame))
        assertEquals(sent.id, valid.id)
        assertEquals(Bytes(sent.frame), valid.received)
        assertEquals(sent.report, assertIs<PublicReportFrame>(valid.frame).toReport())
    }

    @Test
    fun `flipping any bit outside the hop budget is refused`() {
        val frame = sent().frame
        for (i in frame.indices) {
            if (i == Wire.HOP_BUDGET_OFFSET) continue
            for (bit in 0 until 8) {
                val tampered = frame.copyOf().also { it[i] = (it[i].toInt() xor (1 shl bit)).toByte() }
                val result = validate(tampered)
                assertIs<Validation.Rejected>(result, "byte $i bit $bit was accepted")
            }
        }
    }

    @Test
    fun `a relay may change the hop budget, and the id does not change`() {
        val sent = sent()
        val relayed = sent.frame.copyOf().also { it[Wire.HOP_BUDGET_OFFSET] = 2 }
        val valid = assertIs<Validation.Valid>(validate(relayed))
        assertEquals(sent.id, valid.id)
        assertEquals(2, valid.frame.hopBudget)
    }

    @Test
    fun `a report replayed after its expiry and grace is refused`() {
        val sent = sent(ReportType.TEAR_GAS) // 15 minutes
        val created = Instant.parse("2026-11-18T10:00:00Z")
        assertIs<Validation.Valid>(validate(sent.frame, at = created + 16.minutes))
        assertEquals(FrameRejection.EXPIRED, rejection(validate(sent.frame, at = created + 17.minutes)))
        assertEquals(FrameRejection.EXPIRED, rejection(validate(sent.frame, at = created + 3.minutes * 60)))
    }

    @Test
    fun `a report from more than five minutes in the future is refused`() {
        val sent = sent()
        val created = Instant.parse("2026-11-18T10:00:00Z")
        assertIs<Validation.Valid>(validate(sent.frame, at = created - 5.minutes))
        val result = assertIs<Validation.Rejected>(validate(sent.frame, at = created - 6.minutes))
        assertEquals(FrameRejection.FROM_THE_FUTURE, result.reason)
        assertEquals(created.wireMinute(), result.createdMinute)
    }

    @Test
    fun `a report already held is a duplicate, before its signature is checked`() {
        val sent = sent()
        var asked: Bytes? = null
        assertEquals(
            FrameRejection.DUPLICATE,
            rejection(
                validate(sent.frame) {
                    asked = it
                    true
                },
            ),
        )
        assertEquals(sent.id, asked)
    }

    @Test
    fun `a report re-signed under another key, or carrying another key, is refused`() {
        val a = decodeReport(sent().frame)
        val b = decodeReport(sent(ReportType.MEDIC).frame)
        // A's content under B's key and signature: the signature is B's, over other bytes.
        val swappedKey = WireCodec.encode(a.copy(publicKey = b.publicKey, signature = b.signature))
        assertEquals(FrameRejection.BAD_SIGNATURE, rejection(validate(swappedKey)))
        // A's signature with B's key.
        val wrongKey = WireCodec.encode(a.copy(publicKey = b.publicKey))
        assertEquals(FrameRejection.BAD_SIGNATURE, rejection(validate(wrongKey)))
        // A frame that was never signed.
        val unsigned = WireCodec.encode(a.copy(signature = Bytes(ByteArray(Wire.SIGNATURE_BYTES))))
        assertEquals(FrameRejection.BAD_SIGNATURE, rejection(validate(unsigned)))
    }

    @Test
    fun `malformed frames carry the decoder's reason`() {
        val result = assertIs<Validation.Rejected>(validate(byteArrayOf(9, 1, 0)))
        assertEquals(FrameRejection.MALFORMED, result.reason)
        assertEquals(Rejection.UNKNOWN_VERSION, result.wire)
        assertEquals(FrameRejection.MALFORMED, rejection(validate(ByteArray(0))))
    }

    @Test
    fun `a group frame is held by its id, unread, and a second copy is a duplicate`() {
        val group =
            ByteArray(Wire.GROUP_FRAME_BYTES).also {
                it[0] = 1
                it[1] = 4
                it[Wire.HOP_BUDGET_OFFSET] = 3
            }
        val opaque = assertIs<Validation.Opaque>(validate(group))
        val relayed = group.copyOf().also { it[Wire.HOP_BUDGET_OFFSET] = 2 }
        assertEquals(opaque.id, assertIs<Validation.Opaque>(validate(relayed)).id, "the id ignores the hop budget")
        assertEquals(FrameRejection.DUPLICATE, rejection(FrameValidator.validate(group, 0, seen = { it == opaque.id })))
    }

    @Test
    fun `a join step is never stored`() {
        val join =
            ByteArray(Wire.GROUP_FRAME_BYTES).also {
                it[0] = 1
                it[1] = 6
                it[Wire.HEADER_BYTES + Wire.JOIN_SESSION_BYTES] = 1
            }
        assertEquals(FrameRejection.UNSUPPORTED_KIND, rejection(validate(join)))
    }

    @Test
    fun `a withdrawal verifies, and removes only the report its key signed`() {
        val mine = sent()
        val other = sent(ReportType.MEDIC)
        val withdrawal = assertIs<Validation.Valid>(validate(author.withdraw(mine.id, hopBudget = 5)!!))
        val w = assertIs<WithdrawalFrame>(withdrawal.frame)
        val mineFrame = decodeReport(mine.frame)
        val otherFrame = decodeReport(other.frame)
        assertTrue(FrameValidator.withdraws(w, mineFrame, mine.id))
        assertEquals(false, FrameValidator.withdraws(w, otherFrame, other.id))
        // A stranger's withdrawal naming my report, signed with their own key, removes nothing.
        val strangerKeyed = w.copy(publicKey = otherFrame.publicKey)
        assertEquals(false, FrameValidator.withdraws(strangerKeyed, mineFrame, mine.id))
    }

    private fun decodeReport(bytes: ByteArray): PublicReportFrame =
        assertIs<PublicReportFrame>(assertIs<DecodeResult.Accepted>(WireCodec.decode(bytes)).frame)
}
