package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.diagnostics.DiagnosticsFile
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Field diagnostics on a running mesh (WI-036, NFR-19, AC-28's automated half). */
class DiagnosticsMeshTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-19T15:00:00Z")

    @Test
    fun `a session of reports, groups and positions exports counts only`() =
        runTest {
            val start = testScheduler.currentTime
            val network = LoopbackNetwork(random = Random(5), scope = backgroundScope, clock = { testScheduler.currentTime - start })
            val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
            val phones = List(3) { network.phone() }
            network.link(phones[0], phones[1])
            network.link(phones[1], phones[2])
            val (a, b, c) = phones.mapIndexed { i, phone -> MeshNode(phone, backgroundScope, clock, Random(50 + i)) }
            assertFalse(b.diagnosticsOn.value, "off by default")
            assertNull(b.diagnosticsExport())
            b.startDiagnostics()
            listOf(a, b, c).forEach { it.turnOn() }
            delay(1.seconds)

            val group = assertNotNull(a.groupChannel!!.create("AuditCrew", "Yellow Otter"))
            assertNotNull(b.groupChannel!!.join(a.groupChannel!!.invite(group.id)!!, "Blue Heron"))
            assertTrue(a.send(ReportType.TEAR_GAS, here))
            delay(MeshNode.SEND_GRACE + 2.seconds)
            assertTrue(a.send(ReportType.MEDIC, here, group = group.id))
            delay(MeshNode.SEND_GRACE + 2.seconds)
            a.updatePosition(here)
            assertTrue(a.startSharing(group.id, until = null))
            b.batteryLevel(77)
            delay(2.minutes)

            val file = assertNotNull(b.diagnosticsExport())
            for (secret in listOf("AuditCrew", "Yellow Otter", "Blue Heron", "48.86", "2.36", "TEAR_GAS", "MEDIC", "2026")) {
                assertFalse(secret in file, "the export holds \"$secret\"")
            }
            val headers = file.lines().filter { it.isNotEmpty() && !it.first().isDigit() }.drop(2)
            for (line in headers) {
                assertTrue(line.split(",").all { it in DiagnosticsFile.FIELDS || it.matches(Regex("""\d+|yes|no""")) }, line)
            }
            val rows =
                file
                    .lines()
                    .dropWhile { it != DiagnosticsFile.MINUTE_HEADER.joinToString(",") }
                    .drop(1)
                    .takeWhile { it.isNotEmpty() }
                    .map { row -> row.split(",").map(String::toInt) }
            assertTrue(rows.sumOf { it[1] } >= 2, "B heard both its neighbours: $file")
            assertTrue(rows.sumOf { it[2] } >= 1, "B received the report: $file")
            assertTrue(rows.sumOf { it[3] } >= 1, "B relayed it towards C: $file")
            assertTrue("0,77" in file.lines(), "battery at minute 0")

            b.stopDiagnostics()
            assertNull(b.diagnosticsExport(), "turning off forgets every count (FR-17.4)")
            b.startDiagnostics()
            assertEquals(0, b.diagnosticsExport()!!.lines().count { it.startsWith("0,") }, "a new session starts empty")
            b.wipe()
            assertFalse(b.diagnosticsOn.value, "the panic wipe forgets them too")
        }
}
