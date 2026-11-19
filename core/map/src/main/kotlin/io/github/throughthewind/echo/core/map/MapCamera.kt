package io.github.throughthewind.echo.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.throughthewind.echo.core.model.GeoPoint
import org.oscim.core.Point
import org.oscim.core.GeoPoint as VtmGeoPoint
import org.oscim.map.Map as VtmMap

/**
 * The map's view, for what the app draws over it in Compose: pins and the report crosshair
 * (screens/MapView). Reading [toScreen] or [center] subscribes the caller to map moves, so an
 * overlay follows the map as it pans and zooms. Main thread only.
 */
@Stable
class MapCamera {
    internal var map: VtmMap? = null
        set(value) {
            field = value
            moved()
        }

    private var moves by mutableIntStateOf(0)
    private val point = Point()

    internal fun moved() {
        moves++
    }

    /** Where [position] is, in pixels from the map's top left, or null while no map is drawn. */
    fun toScreen(position: GeoPoint): Offset? {
        moves
        val map = map ?: return null
        map.viewport().toScreenPoint(VtmGeoPoint(position.latitude, position.longitude), false, point)
        return Offset(point.x.toFloat(), point.y.toFloat())
    }

    /**
     * The place under [offset], in pixels from the map's top left, or null with no map. The report
     * crosshair sits above the picker sheet, not at the view's centre, so it asks here (FR-4.2).
     */
    fun fromScreen(offset: Offset): GeoPoint? {
        moves
        val map = map ?: return null
        val place = map.viewport().fromScreenPoint(offset.x, offset.y)
        return GeoPoint(place.latitude.coerceIn(-90.0, 90.0), place.longitude.coerceIn(-180.0, 180.0))
    }

    /** The point at the centre of the map, where a report is dropped (FR-4.2), or null with no map. */
    fun center(): GeoPoint? {
        moves
        val position = map?.mapPosition ?: return null
        return GeoPoint(position.latitude.coerceIn(-90.0, 90.0), position.longitude.coerceIn(-180.0, 180.0))
    }

    /** Pans to [position]; only when the user asks (centre on me), never on its own. */
    fun centreOn(position: GeoPoint) {
        map?.animator()?.animateTo(VtmGeoPoint(position.latitude, position.longitude))
    }
}

@Composable
fun rememberMapCamera(): MapCamera = remember { MapCamera() }
