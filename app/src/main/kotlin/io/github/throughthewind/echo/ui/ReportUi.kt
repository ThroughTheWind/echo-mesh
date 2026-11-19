package io.github.throughthewind.echo.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.Expiry
import io.github.throughthewind.echo.core.designsystem.component.PinFamily
import io.github.throughthewind.echo.core.designsystem.component.PinLook
import io.github.throughthewind.echo.core.designsystem.component.PinSource
import io.github.throughthewind.echo.core.designsystem.component.timeLeftLabel
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.model.Compass
import io.github.throughthewind.echo.core.model.Geo
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/** The family a type belongs to (foundations/Colour, tokens.source.json `families`). */
val ReportType.family: PinFamily
    get() =
        when (this) {
            ReportType.POLICE_LINE, ReportType.KETTLE, ReportType.TEAR_GAS, ReportType.ARRESTS -> PinFamily.Hazard
            ReportType.STREET_CLOSED -> PinFamily.Blocked
            ReportType.MEDIC, ReportType.WATER_SUPPLIES, ReportType.EXIT_OPEN, ReportType.ALL_CLEAR -> PinFamily.Safe
        }

/** One glyph per type (foundations/Iconography). */
@get:DrawableRes
val ReportType.icon: Int
    get() =
        when (this) {
            ReportType.POLICE_LINE -> DS.drawable.ic_local_police
            ReportType.KETTLE -> DS.drawable.ic_fence
            ReportType.TEAR_GAS -> DS.drawable.ic_foggy
            ReportType.ARRESTS -> DS.drawable.ic_person_alert
            ReportType.STREET_CLOSED -> DS.drawable.ic_block
            ReportType.MEDIC -> DS.drawable.ic_medical_services
            ReportType.WATER_SUPPLIES -> DS.drawable.ic_water_drop
            ReportType.EXIT_OPEN -> DS.drawable.ic_door_open
            ReportType.ALL_CLEAR -> DS.drawable.ic_check_circle
        }

@get:StringRes
val ReportType.label: Int
    get() =
        when (this) {
            ReportType.POLICE_LINE -> R.string.type_police_line
            ReportType.KETTLE -> R.string.type_kettle
            ReportType.TEAR_GAS -> R.string.type_tear_gas
            ReportType.ARRESTS -> R.string.type_arrests
            ReportType.MEDIC -> R.string.type_medic
            ReportType.WATER_SUPPLIES -> R.string.type_water_supplies
            ReportType.EXIT_OPEN -> R.string.type_exit_open
            ReportType.STREET_CLOSED -> R.string.type_street_closed
            ReportType.ALL_CLEAR -> R.string.type_all_clear
        }

/** The picker's order: grouped by family, hazards first (patterns/ReportPicker). */
val pickerOrder: List<ReportType> =
    ReportType.entries.sortedBy { it.family.ordinal }

/** What a held report's pin looks like now. Every report is public until groups arrive (WI-022). */
fun ShownReport.look(now: Instant): PinLook =
    PinLook(
        family = report.type.family,
        icon = report.type.icon,
        source = PinSource.Nearby,
        timeLeft = Expiry.fraction(report.expiresAt - now, report.lifetime),
        mine = mine,
    )

/** "{Type}, {source}, {N} minutes left" (components/ReportPin). */
@Composable
fun ShownReport.describe(now: Instant): String =
    stringResource(
        R.string.pin_described,
        stringResource(report.type.label),
        stringResource(if (mine) R.string.source_yours else R.string.source_nearby),
        timeLeftLabel(report.expiresAt - now),
    )

@Composable
fun Compass.word(): String =
    stringResource(
        when (this) {
            Compass.NORTH -> R.string.direction_north
            Compass.NORTH_EAST -> R.string.direction_north_east
            Compass.EAST -> R.string.direction_east
            Compass.SOUTH_EAST -> R.string.direction_south_east
            Compass.SOUTH -> R.string.direction_south
            Compass.SOUTH_WEST -> R.string.direction_south_west
            Compass.WEST -> R.string.direction_west
            Compass.NORTH_WEST -> R.string.direction_north_west
        },
    )

/** "About 120 m north-east", from [from]. */
@Composable
fun distanceFrom(
    from: GeoPoint,
    to: GeoPoint,
): String = stringResource(R.string.distance_direction, Geo.roundedMeters(Geo.distanceMeters(from, to)), Geo.compass(from, to).word())

/** "reported 6 min ago". */
@Composable
fun reportedAgo(
    createdAt: Instant,
    now: Instant,
): String {
    val minutes = (now - createdAt).inWholeMinutes.toInt()
    return if (minutes < 1) stringResource(R.string.reported_now) else stringResource(R.string.reported_ago, minutes)
}
