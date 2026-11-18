package io.github.throughthewind.echo.core.map

import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream

/** The part of a geographic box a map file covers, in degrees. */
data class GeoBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
) {
    val centerLatitude: Double get() = (minLatitude + maxLatitude) / 2
    val centerLongitude: Double get() = (minLongitude + maxLongitude) / 2
}

/**
 * The fixed start of a Mapsforge `.map` file (format versions 3 to 5), read to check that a file the
 * user imports is a map before the renderer opens it, and to know where it is. Everything else in
 * the file is the renderer's to read.
 */
data class MapFileHeader(
    val version: Int,
    /** The size the file says it has; an import whose size differs was cut short. */
    val fileSize: Long,
    val createdMillis: Long,
    val box: GeoBox,
) {
    sealed interface Result {
        data class Valid(
            val header: MapFileHeader,
        ) : Result

        data class Invalid(
            val reason: String,
        ) : Result
    }

    companion object {
        private val MAGIC = "mapsforge binary OSM".encodeToByteArray()
        private val VERSIONS = 3..5
        private const val MICRODEGREES = 1_000_000.0

        fun read(file: File): Result = file.inputStream().buffered().use(::read)

        fun read(input: InputStream): Result =
            try {
                val data = DataInputStream(input)
                val magic = ByteArray(MAGIC.size).also(data::readFully)
                if (!magic.contentEquals(MAGIC)) {
                    Result.Invalid("not a Mapsforge map file")
                } else {
                    data.readInt() // Remaining header size.
                    val version = data.readInt()
                    val size = data.readLong()
                    val created = data.readLong()
                    val box =
                        GeoBox(
                            minLatitude = data.readInt() / MICRODEGREES,
                            minLongitude = data.readInt() / MICRODEGREES,
                            maxLatitude = data.readInt() / MICRODEGREES,
                            maxLongitude = data.readInt() / MICRODEGREES,
                        )
                    when {
                        version !in VERSIONS -> Result.Invalid("unsupported map file version $version")
                        size <= 0 -> Result.Invalid("impossible file size $size")
                        !box.isValid() -> Result.Invalid("impossible bounding box")
                        else -> Result.Valid(MapFileHeader(version, size, created, box))
                    }
                }
            } catch (_: EOFException) {
                Result.Invalid("file too short")
            } catch (e: IOException) {
                Result.Invalid("could not read: ${e.javaClass.simpleName}")
            }

        private fun GeoBox.isValid() =
            minLatitude in -90.0..90.0 &&
                maxLatitude in -90.0..90.0 &&
                minLongitude in -180.0..180.0 &&
                maxLongitude in -180.0..180.0 &&
                minLatitude < maxLatitude &&
                minLongitude < maxLongitude
    }
}
