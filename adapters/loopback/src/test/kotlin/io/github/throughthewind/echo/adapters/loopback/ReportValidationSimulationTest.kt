package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.report.FrameRejection
import io.github.throughthewind.echo.core.mesh.report.FrameValidator
import io.github.throughthewind.echo.core.mesh.report.ReportAuthor
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.wireMinute
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Spec AC-10 on the simulator, for public reports: an honest phone and a hostile phone send to the
 * same receiver, and only the honest report survives validation. Relaying is WI-009's.
 */
class ReportValidationSimulationTest {
    @Test
    fun `tampered, expired and forged reports from a hostile phone are refused, the honest one is not`() =
        runTest {
            var now = Instant.parse("2026-11-18T10:00:00Z")
            val network = LoopbackNetwork()
            val honest = network.phone()
            val hostile = network.phone()
            val receiver = network.phone()
            network.link(honest, receiver)
            network.link(hostile, receiver)
            listOf(honest, hostile, receiver).forEach { it.start() }

            val here = GeoPoint(48.8675, 2.3632)
            val author = ReportAuthor { now }
            val old = assertIs<ReportAuthor.Outcome.Sent>(author.report(ReportType.TEAR_GAS, here, hopBudget = 5))
            now += 30.minutes
            val fresh = assertIs<ReportAuthor.Outcome.Sent>(author.report(ReportType.MEDIC, here, hopBudget = 5))

            // The hostile phone heard both and now plays them back wrong.
            val tampered = fresh.frame.copyOf().also { it[Wire.HEADER_BYTES + Wire.PUBLIC_KEY_BYTES] = ReportType.ALL_CLEAR.code.toByte() }
            val replayed = old.frame
            val forgedKey = fresh.frame.copyOf().also { it[Wire.HEADER_BYTES] = (it[Wire.HEADER_BYTES].toInt() xor 1).toByte() }
            honest.broadcast(fresh.frame)
            listOf(tampered, replayed, forgedKey).forEach { hostile.broadcast(it) }

            val verdicts =
                network.trace
                    .deliveries()
                    .filter { it.to == receiver.index }
                    .map { it.from to FrameValidator.validate(it.frame.toByteArray(), now.wireMinute()) }
            assertEquals(4, verdicts.size)
            val (fromHonest, fromHostile) = verdicts.partition { it.first == honest.index }
            assertEquals(fresh.id, assertIs<Validation.Valid>(fromHonest.single().second).id)
            assertEquals(
                listOf(FrameRejection.BAD_SIGNATURE, FrameRejection.EXPIRED, FrameRejection.BAD_SIGNATURE),
                fromHostile.map { assertIs<Validation.Rejected>(it.second).reason },
            )
        }
}
