package io.github.throughthewind.echo.core.map

import java.io.File
import java.io.IOException
import java.io.InputStream

/** A map file this phone holds (spec FR-2.6). */
data class MapEntry(
    /** The file's name in the library, without the `.map` extension. */
    val name: String,
    val file: File,
    val sizeBytes: Long,
    val box: GeoBox,
)

/**
 * The map files the user imported (spec FR-2.2 to FR-2.6, ADR-0006), kept in the app's private
 * storage. A file arrives through the system file picker, from a browser download or from another
 * phone; nothing here touches the network. A file is checked to be a whole Mapsforge map before it
 * replaces anything, so a cut-short or wrong file never reaches the renderer.
 */
class MapLibrary(
    private val directory: File,
) {
    sealed interface Imported {
        data class Added(
            val entry: MapEntry,
        ) : Imported

        data class Refused(
            val reason: String,
        ) : Imported
    }

    init {
        directory.mkdirs()
        require(directory.isDirectory) { "not a directory: $directory" }
    }

    /**
     * Copies [input] in as a map named after [displayName], or refuses it. A map with the same name
     * is replaced only once the new one is checked.
     */
    fun import(
        input: InputStream,
        displayName: String,
    ): Imported {
        val name = nameFor(displayName)
        val partial = File(directory, "$name$PARTIAL")
        try {
            partial.outputStream().use { out -> input.copyTo(out) }
            val header =
                when (val result = MapFileHeader.read(partial)) {
                    is MapFileHeader.Result.Invalid -> return refuse(partial, result.reason)
                    is MapFileHeader.Result.Valid -> result.header
                }
            if (header.fileSize != partial.length()) {
                return refuse(partial, "the file is incomplete: ${partial.length()} of ${header.fileSize} bytes")
            }
            val target = File(directory, "$name$EXTENSION")
            if (!partial.renameTo(target)) return refuse(partial, "could not store the map")
            return Imported.Added(MapEntry(name, target, target.length(), header.box))
        } catch (e: IOException) {
            return refuse(partial, "could not copy the file: ${e.javaClass.simpleName}")
        }
    }

    /** Every usable map, by name. A file that stopped being a valid map is left out. */
    fun list(): List<MapEntry> =
        directory
            .listFiles { f -> f.isFile && f.name.endsWith(EXTENSION) }
            .orEmpty()
            .mapNotNull { file ->
                val header = (MapFileHeader.read(file) as? MapFileHeader.Result.Valid)?.header ?: return@mapNotNull null
                MapEntry(file.name.removeSuffix(EXTENSION), file, file.length(), header.box)
            }.sortedBy { it.name }

    fun delete(name: String): Boolean = File(directory, "$name$EXTENSION").let { it.isFile && it.delete() }

    /** Deletes every map and every partial import: for the panic wipe, when the user opts in (FR-2.6). */
    fun deleteAll() {
        directory.listFiles().orEmpty().forEach { it.delete() }
    }

    private fun refuse(
        partial: File,
        reason: String,
    ): Imported.Refused {
        partial.delete()
        return Imported.Refused(reason)
    }

    companion object {
        const val EXTENSION = ".map"
        private const val PARTIAL = ".part"

        /** A safe file name from what the picker calls the file: `Île-de-France.map` → `le-de-france`. */
        fun nameFor(displayName: String): String {
            val stem = displayName.substringAfterLast('/').removeSuffix(EXTENSION).lowercase()
            val safe = stem.replace(Regex("[^a-z0-9]+"), "-").trim('-').take(64)
            return safe.ifEmpty { "map" }
        }
    }
}
