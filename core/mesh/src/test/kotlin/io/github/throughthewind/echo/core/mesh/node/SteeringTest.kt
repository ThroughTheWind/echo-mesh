package io.github.throughthewind.echo.core.mesh.node

import io.github.throughthewind.echo.core.mesh.report.Tally
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.Report
import io.github.throughthewind.echo.core.model.ReportType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Steering reports (WI-034, FR-15, AC-26's automated half). */
class SteeringTest {
    private val created = Instant.parse("2026-11-19T16:00:00Z")
    private val votes = Tally(groupConfirmed = 1, nearbyConfirmed = 40, expiresMinute = 0)

    private fun shown(
        type: ReportType,
        group: GroupTag? = null,
        mine: Boolean = false,
    ) = ShownReport(
        id = Bytes(ByteArray(32)),
        report = Report(type, GeoPoint(48.8675, 2.3632), created, 15.minutes),
        hops = 2,
        mine = mine,
        group = group,
        tally = votes,
    )

    private val crew = GroupTag(Bytes(ByteArray(16)), "Crew", "Owl")

    @Test
    fun `AC-26 a stranger's public steering report warns and shows only group confirmations`() {
        for (type in ReportType.entries.filter { it.steering }) {
            val report = shown(type)
            assertTrue(report.couldBeATrap, "$type")
            assertTrue(report.publicConfirmationsHidden, "$type")
            assertEquals(1, report.confirmationsShown, "$type: the group member's confirmation shows, the 40 public ones do not")
        }
    }

    @Test
    fun `a non-steering public report keeps its public count and no warning`() {
        for (type in ReportType.entries.filterNot { it.steering }) {
            val report = shown(type)
            assertFalse(report.couldBeATrap, "$type")
            assertEquals(41, report.confirmationsShown, "$type")
        }
    }

    @Test
    fun `a group's steering report is not a stranger's, and your own carries no warning but no public count`() {
        val fromGroup = shown(ReportType.EXIT_OPEN, group = crew)
        assertFalse(fromGroup.couldBeATrap)
        assertEquals(41, fromGroup.confirmationsShown)
        val mine = shown(ReportType.ALL_CLEAR, mine = true)
        assertFalse(mine.couldBeATrap)
        assertEquals(1, mine.confirmationsShown)
    }
}
