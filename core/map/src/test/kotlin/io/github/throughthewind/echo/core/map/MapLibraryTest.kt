package io.github.throughthewind.echo.core.map

import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MapLibraryTest {
    @TempDir
    lateinit var dir: File

    /** A map file header for Paris, padded to [size] bytes; the renderer never reads these. */
    private fun mapFile(
        size: Int = 200,
        version: Int = 5,
        declaredSize: Long = size.toLong(),
        minLat: Int = 48_800_000,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            write("mapsforge binary OSM".encodeToByteArray())
            writeInt(100)
            writeInt(version)
            writeLong(declaredSize)
            writeLong(1_763_000_000_000)
            writeInt(minLat)
            writeInt(2_200_000)
            writeInt(48_920_000)
            writeInt(2_470_000)
        }
        return out.toByteArray().copyOf(size)
    }

    @Test
    fun `a whole map file is imported under a safe name and listed with its size and box`() {
        val library = MapLibrary(dir)
        val added = assertIs<MapLibrary.Imported.Added>(library.import(mapFile().inputStream(), "Île-de-France.map"))
        assertEquals("le-de-france", added.entry.name)
        val listed = library.list().single()
        assertEquals(200, listed.sizeBytes)
        assertEquals(48.8, listed.box.minLatitude, 1e-9)
        assertEquals(2.335, listed.box.centerLongitude, 1e-9)
    }

    @Test
    fun `a file that is not a map, or is cut short, is refused and leaves nothing behind`() {
        val library = MapLibrary(dir)
        val notAMap = library.import("<html>".encodeToByteArray().inputStream(), "paris.map")
        assertIs<MapLibrary.Imported.Refused>(notAMap)
        val cut = library.import(mapFile(size = 200, declaredSize = 10_000).inputStream(), "paris.map")
        assertTrue(assertIs<MapLibrary.Imported.Refused>(cut).reason.contains("incomplete"))
        val oldVersion = library.import(mapFile(version = 2).inputStream(), "paris.map")
        assertIs<MapLibrary.Imported.Refused>(oldVersion)
        val badBox = library.import(mapFile(minLat = 95_000_000).inputStream(), "paris.map")
        assertIs<MapLibrary.Imported.Refused>(badBox)
        assertEquals(emptyList(), library.list())
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test
    fun `a refused import does not replace a good map of the same name`() {
        val library = MapLibrary(dir)
        library.import(mapFile().inputStream(), "paris.map")
        library.import("junk".encodeToByteArray().inputStream(), "paris.map")
        assertEquals(listOf("paris"), library.list().map { it.name })
    }

    @Test
    fun `maps can be deleted one by one or all at once`() {
        val library = MapLibrary(dir)
        library.import(mapFile().inputStream(), "paris.map")
        library.import(mapFile().inputStream(), "lyon.map")
        assertTrue(library.delete("lyon"))
        assertEquals(listOf("paris"), library.list().map { it.name })
        library.deleteAll()
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test
    fun `names never leave the library`() {
        assertEquals("passwd", MapLibrary.nameFor("../../etc/passwd"))
        assertEquals("map", MapLibrary.nameFor("...map"))
        assertEquals("berlin-2026", MapLibrary.nameFor("Berlin 2026.map"))
    }
}
