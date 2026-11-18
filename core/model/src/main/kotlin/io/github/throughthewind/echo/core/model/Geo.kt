package io.github.throughthewind.echo.core.model

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** The eight compass words the screens use for a direction (foundations/ContentAndVoice). */
enum class Compass { NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST }

/** Distances and directions between points, at street scale, as the screens say them. */
object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_008.8
    private const val SMALL_STEP = 10
    private const val LARGE_STEP = 50
    private const val SMALL_UNDER = 100
    private const val DEGREES_PER_POINT = 45.0

    /** Great-circle distance in metres (haversine). */
    fun distanceMeters(
        from: GeoPoint,
        to: GeoPoint,
    ): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Initial bearing from [from] to [to], degrees clockwise from north, 0 to 360. */
    fun bearingDegrees(
        from: GeoPoint,
        to: GeoPoint,
    ): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun compass(
        from: GeoPoint,
        to: GeoPoint,
    ): Compass = Compass.entries[((bearingDegrees(from, to) / DEGREES_PER_POINT).roundToInt()) % Compass.entries.size]

    /**
     * A distance as the screens say it: 10 m steps under 100 m, 50 m steps above, so it never
     * claims more precision than a phone's position has.
     */
    fun roundedMeters(meters: Double): Int {
        val step = if (meters < SMALL_UNDER) SMALL_STEP else LARGE_STEP
        return ((meters / step).roundToInt() * step).coerceAtLeast(SMALL_STEP)
    }
}
