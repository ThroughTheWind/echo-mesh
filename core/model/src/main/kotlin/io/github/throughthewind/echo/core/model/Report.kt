package io.github.throughthewind.echo.core.model

import kotlin.time.Duration
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
 * What a report says. The set is a placeholder from the brief; the spec owns the final list and its
 * default time-to-live per kind.
 */
enum class ReportKind {
    POLICE_LINE,
    TEAR_GAS,
    MEDIC,
    WATER,
    EXIT_OPEN,
    STREET_CLOSED,
}

/**
 * A short-lived, map-pinned report. Reports always expire: the app keeps no location history
 * (brief, constraint 5), so there is no report without a time-to-live.
 */
data class Report(
    val kind: ReportKind,
    val position: GeoPoint,
    val createdAt: Instant,
    val timeToLive: Duration,
) {
    init {
        require(timeToLive.isPositive()) { "a report must expire: time-to-live was $timeToLive" }
    }

    val expiresAt: Instant get() = createdAt + timeToLive

    fun isExpired(now: Instant): Boolean = now >= expiresAt
}
