package io.github.throughthewind.echo.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoTest {
    private val place = GeoPoint(48.8675, 2.3632)

    @Test
    fun `distances at street scale are close to the truth`() {
        // 0.001 degree of latitude is about 111 m.
        val north = GeoPoint(48.8685, 2.3632)
        val d = Geo.distanceMeters(place, north)
        assertTrue(d in 110.0..112.5, "was $d")
    }

    @Test
    fun `directions are one of eight compass words`() {
        assertEquals(Compass.NORTH, Geo.compass(place, GeoPoint(48.8685, 2.3632)))
        assertEquals(Compass.EAST, Geo.compass(place, GeoPoint(48.8675, 2.3652)))
        assertEquals(Compass.SOUTH_WEST, Geo.compass(place, GeoPoint(48.8665, 2.3617)))
        assertEquals(Compass.NORTH, Geo.compass(place, GeoPoint(48.8685, 2.3631)), "a hair west of north is still north")
    }

    @Test
    fun `distances are rounded to 10 m under 100 m and to 50 m above, never to zero`() {
        assertEquals(10, Geo.roundedMeters(2.0))
        assertEquals(40, Geo.roundedMeters(37.0))
        assertEquals(100, Geo.roundedMeters(99.0))
        assertEquals(100, Geo.roundedMeters(112.0))
        assertEquals(150, Geo.roundedMeters(131.0))
    }
}
