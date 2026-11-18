package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.mesh.report.ReportAuthor.Outcome
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ReportAuthorTest {
    private var now = Instant.parse("2026-11-18T10:00:42.250Z")
    private val author = ReportAuthor { now }
    private val here = GeoPoint(48.867_54, 2.363_249)

    private fun sent(
        type: ReportType = ReportType.TEAR_GAS,
        at: GeoPoint = here,
    ) = assertIs<Outcome.Sent>(author.report(type, at, hopBudget = 5))

    private fun decode(bytes: ByteArray) = assertIs<DecodeResult.Accepted>(WireCodec.decode(bytes)).frame

    @Test
    fun `a report is cut to the minute and the wire's grid before it is signed`() {
        val sent = sent()
        val frame = assertIs<PublicReportFrame>(decode(sent.frame))
        assertEquals(Instant.parse("2026-11-18T10:00:00Z"), sent.report.createdAt)
        assertEquals(488_675, frame.latitude)
        assertEquals(23_632, frame.longitude)
        assertEquals(GeoPoint(48.8675, 2.3632), sent.report.position)
        assertEquals(ReportType.TEAR_GAS.defaultLifetime, sent.report.lifetime)
        assertEquals(Wire.PUBLIC_KEY_BYTES, frame.publicKey.size)
    }

    @Test
    fun `two reports from one phone share no key and no signature, only what they say`() {
        val first = sent(ReportType.MEDIC).frame
        now += 2.minutes
        // The same content again, a key rotation later: everything but the key and the signature is
        // fixed by what the report says, so the bytes cannot link the two to one phone.
        val again = sent(ReportType.MEDIC).frame
        now -= 2.minutes
        val a = assertIs<PublicReportFrame>(decode(first))
        val b = assertIs<PublicReportFrame>(decode(again))
        assertNotEquals(a.publicKey, b.publicKey)
        assertNotEquals(a.signature, b.signature)
        val keyRange = Wire.HEADER_BYTES until Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES
        val signatureRange = first.size - Wire.SIGNATURE_BYTES until first.size
        val created = Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES + 1 + 4 + 4 until Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES + 1 + 4 + 4 + 4
        val differing = first.indices.filter { first[it] != again[it] }
        assertTrue(differing.all { it in keyRange || it in signatureRange || it in created }, "bytes differ at $differing")
    }

    @Test
    fun `the same type twice within a minute is refused, another type is not`() {
        sent(ReportType.POLICE_LINE)
        assertEquals(Outcome.RateLimited, author.report(ReportType.POLICE_LINE, here, hopBudget = 5))
        sent(ReportType.MEDIC)
        now += 59.seconds
        assertEquals(Outcome.RateLimited, author.report(ReportType.POLICE_LINE, here, hopBudget = 5))
        now += 1.seconds
        sent(ReportType.POLICE_LINE)
    }

    @Test
    fun `a withdrawal is signed by the report's key and lives until the report would have expired`() {
        val sent = sent(ReportType.STREET_CLOSED) // 30 minutes, created 10:00
        now += 12.minutes
        val w = assertIs<WithdrawalFrame>(decode(author.withdraw(sent.id, hopBudget = 4)!!))
        val r = assertIs<PublicReportFrame>(decode(sent.frame))
        assertEquals(r.publicKey, w.publicKey)
        assertEquals(sent.id, w.target)
        assertEquals(r.createdMinute + 12, w.createdMinute)
        assertEquals(18, w.lifetimeMinutes)
        assertEquals(4, w.hopBudget)
        // The key is gone once used.
        assertNull(author.withdraw(sent.id, hopBudget = 4))
    }

    @Test
    fun `a report's key is forgotten when the report expires`() {
        val sent = sent(ReportType.EXIT_OPEN) // 15 minutes
        assertEquals(setOf(sent.id), author.withdrawable())
        now += 15.minutes
        assertEquals(emptySet(), author.withdrawable())
        assertNull(author.withdraw(sent.id, hopBudget = 4))
    }

    @Test
    fun `lifetimes and hop budgets outside the wire's ranges are refused`() {
        assertFailsWith<IllegalArgumentException> { author.report(ReportType.MEDIC, here, 5, lifetime = 181.minutes) }
        assertFailsWith<IllegalArgumentException> { author.report(ReportType.MEDIC, here, 5, lifetime = 30.seconds) }
        assertFailsWith<IllegalArgumentException> { author.report(ReportType.MEDIC, here, 5, lifetime = 90.5.minutes) }
        assertFailsWith<IllegalArgumentException> { author.report(ReportType.MEDIC, here, Wire.MAX_HOP_BUDGET + 1) }
        val longest = assertIs<Outcome.Sent>(author.report(ReportType.MEDIC, here, 0, lifetime = 180.minutes))
        assertEquals(180.minutes, longest.report.lifetime)
    }

    @Test
    fun `the poles and the antimeridian survive the trip`() {
        val corners = listOf(GeoPoint(90.0, 180.0), GeoPoint(-90.0, -180.0))
        corners.forEachIndexed { i, point ->
            now += 2.minutes
            val sent = sent(ReportType.entries[i], point)
            assertEquals(point, sent.report.position)
            assertIs<Validation.Valid>(FrameValidator.validate(sent.frame, now.wireMinute()))
        }
    }

    @Test
    fun `the picker can tell when a rate-limited type opens again`() {
        assertNull(author.nextAllowed(ReportType.MEDIC))
        sent(ReportType.MEDIC)
        assertEquals(now + 1.minutes, author.nextAllowed(ReportType.MEDIC))
        assertNull(author.nextAllowed(ReportType.KETTLE))
        now += 1.minutes
        assertNull(author.nextAllowed(ReportType.MEDIC))
    }
}
