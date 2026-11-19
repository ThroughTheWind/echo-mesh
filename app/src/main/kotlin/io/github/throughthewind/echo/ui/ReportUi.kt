package io.github.throughthewind.echo.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.Expiry
import io.github.throughthewind.echo.core.designsystem.component.PinFamily
import io.github.throughthewind.echo.core.designsystem.component.PinLook
import io.github.throughthewind.echo.core.designsystem.component.PinSource
import io.github.throughthewind.echo.core.designsystem.component.timeLeftLabel
import io.github.throughthewind.echo.core.mesh.group.GroupInfo
import io.github.throughthewind.echo.core.mesh.node.ShownReport
import io.github.throughthewind.echo.core.mesh.node.confirmationsShown
import io.github.throughthewind.echo.core.mesh.node.couldBeATrap
import io.github.throughthewind.echo.core.mesh.wire.Bytes
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

/** The groups this phone is in, in order: a group's colour is its place in this list (FR-9.2). */
val LocalGroups = staticCompositionLocalOf { emptyList<GroupInfo>() }

/** A group's colour index, stable while the phone stays in the group. */
fun List<GroupInfo>.colourOf(id: Bytes): Int = indexOfFirst { it.id == id }.coerceAtLeast(0)

/** Lower-case hex, for keeping a group's id in the sealed settings. */
fun Bytes.hex(): String = toByteArray().joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

/** Up to two initials, for a group's or a member's monogram (components/AvatarAndBadge). */
fun initials(name: String): String =
    name
        .split(' ')
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

/**
 * What a held report's pin looks like now: solid with the group's colour and monogram when a member
 * of one of this phone's groups signed it, hollow when anyone nearby could have sent it (FR-9.2).
 */
@Composable
fun ShownReport.look(now: Instant): PinLook {
    val groups = LocalGroups.current
    val tag = group
    return PinLook(
        family = report.type.family,
        icon = report.type.icon,
        source = if (tag == null) PinSource.Nearby else PinSource.Group(groups.colourOf(tag.groupId), initials(tag.groupName)),
        timeLeft = Expiry.fraction(report.expiresAt - now, report.lifetime),
        confirmations = confirmationsShown,
        probablyGone = tally.probablyGone,
        mine = mine,
        couldBeATrap = couldBeATrap,
    )
}

/**
 * "{Type}, {source}, {N} minutes left[, {N} confirmations][, probably gone]" (components/ReportPin):
 * "from Sage in Crew" for a group.
 */
@Composable
fun ShownReport.describe(now: Instant): String {
    val tag = group
    val source =
        when {
            mine -> stringResource(R.string.source_yours)
            tag != null -> stringResource(R.string.source_group, tag.sender, tag.groupName)
            else -> stringResource(R.string.source_nearby)
        }
    val confirmations = confirmationsShown
    return listOfNotNull(
        stringResource(R.string.pin_described, stringResource(report.type.label), source, timeLeftLabel(report.expiresAt - now)),
        if (couldBeATrap) stringResource(R.string.pin_could_be_trap) else null,
        if (confirmations > 0) pluralStringResource(R.plurals.pin_confirmations, confirmations, confirmations) else null,
        if (tally.probablyGone) stringResource(R.string.pin_probably_gone) else null,
    ).joinToString(", ")
}

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
