package io.github.throughthewind.echo.core.mesh.diagnostics

import io.github.throughthewind.echo.core.mesh.LinkFailure
import io.github.throughthewind.echo.core.mesh.PeerHandle
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Field diagnostics (WI-036): the FR-17.1 counts, and an export with nothing else in it (NFR-19). */
class FieldDiagnosticsTest {
    private val start = Instant.parse("2026-11-19T15:07:00Z")

    @Test
    fun `peers are counted once per minute, and only their count is kept`() {
        val d = FieldDiagnostics(start)
        val fox = PeerHandle("a1b2c3d4e5f60718")
        val owl = PeerHandle("0f1e2d3c4b5a6978")
        d.peer(fox, start + 5.seconds)
        d.peer(fox, start + 30.seconds)
        d.peer(owl, start + 40.seconds)
        d.peer(fox, start + 70.seconds)
        val minutes = d.snapshot(start + 2.minutes).minutes
        assertEquals(listOf(0 to 2, 1 to 1), minutes.map { it.minute to it.peers })
        val file = d.export(start + 2.minutes)
        assertFalse(fox.token in file || owl.token in file, "a token never reaches the file")
    }

    @Test
    fun `received, relayed, delay and failures add up by minute`() {
        val d = FieldDiagnostics(start)
        d.received(start + 10.seconds, createdAt = start - 3.minutes)
        d.received(start + 20.seconds, createdAt = null)
        d.relayed(start + 20.seconds, 4)
        d.failed(LinkFailure.CONNECT, start + 3.minutes)
        d.failed(LinkFailure.CONNECT, start + 3.minutes + 1.seconds)
        val s = d.snapshot(start + 4.minutes)
        assertEquals(4, s.length)
        val first = s.minutes.first { it.minute == 0 }
        assertEquals(2, first.received)
        assertEquals(4, first.relayed)
        assertEquals(mapOf(3 to 1), s.delays, "only a readable creation time gives a delay")
        assertEquals(mapOf(LinkFailure.CONNECT to 2), s.minutes.first { it.minute == 3 }.failures)
    }

    @Test
    fun `battery is kept once per 10 minutes`() {
        val d = FieldDiagnostics(start)
        d.battery(90, start)
        d.battery(89, start + 4.minutes)
        d.battery(87, start + 10.minutes)
        d.battery(150, start + 21.minutes)
        assertEquals(listOf(0 to 90, 10 to 87, 21 to 100), d.snapshot(start + 22.minutes).battery)
    }

    @Test
    fun `NFR-19 the export holds the FR-17_1 fields and nothing else`() {
        val d = FieldDiagnostics(start)
        d.killedBeforeStart()
        d.peer(PeerHandle("ffeeddccbbaa9988"), start)
        d.received(start + 1.minutes, start)
        d.relayed(start + 1.minutes, 2)
        LinkFailure.entries.forEach { d.failed(it, start + 2.minutes) }
        d.battery(80, start)
        d.serviceKilled(start + 5.minutes)
        val file = d.export(start + 6.minutes)
        assertExportShape(file)
        assertTrue("killed_before_session,yes" in file)
        assertTrue("5,yes" in file)
        assertFalse(Regex("""\d{4}-\d{2}-\d{2}|\d{1,2}:\d{2}""").containsMatchIn(file), "no date, no time of day")
    }

    @Test
    fun `an empty session still exports its headers`() {
        assertExportShape(FieldDiagnostics(start).export(start))
    }

    companion object {
        /**
         * Every line of [file] is the title, the note, a blank, a key the format knows with a number
         * or yes/no, a header of known fields, or a row of numbers as wide as its header.
         */
        fun assertExportShape(file: String) {
            val lines = file.lines()
            assertEquals(DiagnosticsFile.TITLE, lines[0])
            assertEquals(DiagnosticsFile.NOTE, lines[1])
            var width = 0
            val headers = mutableListOf<List<String>>()
            for (line in lines.drop(2)) {
                if (line.isEmpty()) {
                    width = 0
                    continue
                }
                val cells = line.split(",")
                when {
                    cells.size == 2 && cells[0] in setOf(DiagnosticsFile.LENGTH, DiagnosticsFile.KILLED_BEFORE) -> {
                        assertTrue(cells[1].matches(Regex("""\d+|yes|no""")), line)
                    }

                    width == 0 -> {
                        assertTrue(cells.all { it in DiagnosticsFile.FIELDS }, "unknown field in: $line")
                        headers += cells
                        width = cells.size
                    }

                    else -> {
                        assertEquals(width, cells.size, line)
                        assertTrue(cells.all { it.matches(Regex("""\d+|yes""")) }, "not a count: $line")
                    }
                }
            }
            assertEquals(
                listOf(
                    DiagnosticsFile.MINUTE_HEADER,
                    DiagnosticsFile.DELAY_HEADER,
                    DiagnosticsFile.BATTERY_HEADER,
                    DiagnosticsFile.KILLED_HEADER,
                ),
                headers,
            )
        }
    }
}
