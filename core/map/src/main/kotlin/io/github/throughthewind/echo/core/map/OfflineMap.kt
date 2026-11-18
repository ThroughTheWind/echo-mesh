package io.github.throughthewind.echo.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.oscim.android.MapView
import org.oscim.layers.LocationLayer
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.theme.internal.VtmThemes
import org.oscim.tiling.source.mapfile.MapFileTileSource

/** Where the user is, in degrees, with its accuracy in metres. Shown, never stored (FR-2.5). */
data class OwnFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)

/**
 * The offline map (spec FR-2.1, ADR-0006): VTM drawing one imported Mapsforge file, entirely from
 * the phone, with the user's own position when [ownFix] is not null. Opens centred on the map's
 * region. Pins come with the report screens (WI-013); the street render theme with the design
 * system's tokens replaces VTM's default theme there too.
 */
@Composable
fun OfflineMap(
    map: MapEntry,
    ownFix: OwnFix?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = remember(map.file) { MapView(context) }
    val location = remember(view) { LocationLayer(view.map()).also { view.map().layers().add(it) } }
    val source =
        remember(map.file) {
            MapFileTileSource().apply { setMapFile(map.file.absolutePath) }
        }

    DisposableEffect(view, source) {
        val vectors = view.map().setBaseMap(source)
        view.map().layers().add(BuildingLayer(view.map(), vectors))
        view.map().layers().add(LabelLayer(view.map(), vectors))
        view.map().setTheme(VtmThemes.DEFAULT)
        view.map().setMapPosition(map.box.centerLatitude, map.box.centerLongitude, START_SCALE)
        onDispose {
            view.onDestroy()
            source.close()
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(view, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        view.onResume()
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        view.onPause()
                    }

                    else -> {}
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(factory = { view }, modifier = modifier, update = {
        if (ownFix == null) {
            location.isEnabled = false
        } else {
            location.isEnabled = true
            location.setPosition(ownFix.latitude, ownFix.longitude, ownFix.accuracyMeters)
        }
        view.map().updateMap(true)
    })
}

/** About zoom level 15: a neighbourhood fills the screen. */
private const val START_SCALE = 1.0 * (1 shl 15)
