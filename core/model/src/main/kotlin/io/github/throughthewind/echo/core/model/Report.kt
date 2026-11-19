package io.github.throughthewind.echo.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A point on the map, in WGS 84 degrees. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}

/**
 * The report catalog of spec FR-4.1, in its order, with its default lifetimes (to tune in field
 * tests, WI-028). [code] is the type's place in that order; ADR-0005 puts it on the air, so a new
 * type is a new wire version.
 */
enum class ReportType(
    val code: Int,
    val defaultLifetime: Duration,
) {
    POLICE_LINE(0, 20.minutes),
    KETTLE(1, 20.minutes),
    TEAR_GAS(2, 15.minutes),
    ARRESTS(3, 15.minutes),
    MEDIC(4, 60.minutes),
    WATER_SUPPLIES(5, 90.minutes),
    EXIT_OPEN(6, 15.minutes),
    STREET_CLOSED(7, 30.minutes),
    ALL_CLEAR(8, 15.minutes),
    ;

    companion object {
        fun of(code: Int): ReportType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * A short-lived, map-pinned report, as the app shows it. It carries only what spec FR-4.4 allows:
 * no sender, no device, no free text. Reports always expire: the app keeps no location history, so
 * there is no report without a lifetime.
 */
data class Report(
    val type: ReportType,
    val position: GeoPoint,
    val createdAt: Instant,
    val lifetime: Duration,
) {
    init {
        require(lifetime.isPositive() && lifetime.isFinite()) { "a report must expire: lifetime was $lifetime" }
    }

    val expiresAt: Instant get() = createdAt + lifetime

    fun isExpired(now: Instant): Boolean = now >= expiresAt
}
