package io.github.throughthewind.echo.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ReportTest {
    private val created = Instant.parse("2026-11-16T10:00:00Z")
    private val here = GeoPoint(48.8566, 2.3522)

    @Test
    fun `a report expires exactly at its lifetime`() {
        val report = Report(ReportType.MEDIC, here, created, 15.minutes)
        assertFalse(report.isExpired(created + 14.minutes))
        assertTrue(report.isExpired(created + 15.minutes))
    }

    @Test
    fun `a report that never expires is refused`() {
        assertFailsWith<IllegalArgumentException> { Report(ReportType.WATER_SUPPLIES, here, created, Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { Report(ReportType.WATER_SUPPLIES, here, created, Duration.INFINITE) }
        assertFailsWith<IllegalArgumentException> { Report(ReportType.WATER_SUPPLIES, here, created, (-1).minutes) }
    }

    @Test
    fun `a point off the globe is refused`() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, -181.0) }
    }

    @Test
    fun `the catalog follows FR-4-1 in order, with its default lifetimes`() {
        assertEquals(List(9) { it }, ReportType.entries.map { it.code })
        assertEquals(
            listOf(20, 20, 15, 15, 60, 90, 15, 30, 15),
            ReportType.entries.map { it.defaultLifetime.inWholeMinutes.toInt() },
        )
        assertEquals(ReportType.MEDIC, ReportType.of(4))
        assertEquals(null, ReportType.of(9))
    }
}
