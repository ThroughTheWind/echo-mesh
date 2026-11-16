package io.github.throughthewind.echo.core.model

import kotlin.test.Test
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
    fun `a report expires exactly at its time-to-live`() {
        val report = Report(ReportKind.MEDIC, here, created, 15.minutes)
        assertFalse(report.isExpired(created + 14.minutes))
        assertTrue(report.isExpired(created + 15.minutes))
    }

    @Test
    fun `a report that never expires is refused`() {
        assertFailsWith<IllegalArgumentException> { Report(ReportKind.WATER, here, created, Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { Report(ReportKind.WATER, here, created, Duration.INFINITE.unaryMinus()) }
    }

    @Test
    fun `a point off the globe is refused`() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, -181.0) }
    }
}
