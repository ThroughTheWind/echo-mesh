package io.github.throughthewind.echo.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.throughthewind.echo.core.map.MapEntry
import io.github.throughthewind.echo.core.map.MapLibrary
import io.github.throughthewind.echo.core.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** The maps on the phone, for the screens; the map shown is the one imported last. */
@Stable
class MapsState(
    private val library: MapLibrary,
) {
    var entries by mutableStateOf(library.list())
        private set

    /** Where the map was last centred, so the list can sort by it when location is off. */
    var lastCentre by mutableStateOf<GeoPoint?>(null)

    /** The map drawn on the map screen. Choosing among several is the Maps screen's (WI-012 follow-up). */
    val current: MapEntry? get() = entries.maxByOrNull { it.file.lastModified() }

    /** Imports the file at [uri] from the system picker, and lists the maps again. */
    suspend fun import(
        resolver: ContentResolver,
        uri: Uri,
    ): MapLibrary.Imported {
        val result =
            withContext(Dispatchers.IO) {
                val name = displayName(resolver, uri) ?: uri.lastPathSegment ?: "map"
                try {
                    resolver.openInputStream(uri)?.use { library.import(it, name) }
                        ?: MapLibrary.Imported.Refused("the file could not be opened")
                } catch (_: IOException) {
                    MapLibrary.Imported.Refused("the file could not be read")
                } catch (_: SecurityException) {
                    MapLibrary.Imported.Refused("Echo was not allowed to read it")
                }
            }
        entries = library.list()
        return result
    }

    private fun displayName(
        resolver: ContentResolver,
        uri: Uri,
    ): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
