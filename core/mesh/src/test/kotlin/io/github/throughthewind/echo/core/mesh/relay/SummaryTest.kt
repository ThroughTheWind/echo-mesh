package io.github.throughthewind.echo.core.mesh.relay

import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SummaryTest {
    private val random = Random(3)

    private fun ids(n: Int) = List(n) { Bytes(random.nextBytes(32)) }

    @Test
    fun `every id in the set is found`() {
        val held = ids(256)
        val summary = Summary.of(held, random)
        assertTrue(held.all { Summary.mayContain(summary, it) })
    }

    @Test
    fun `ids outside the set are mostly not found, at a full store`() {
        val summary = Summary.of(ids(256), random)
        val others = ids(10_000)
        val falsePositives = others.count { Summary.mayContain(summary, it) }
        // About 3.7% expected for 256 ids, 1,768 bits and 4 hashes.
        assertTrue(falsePositives < 600, "$falsePositives false positives in 10,000")
    }

    @Test
    fun `an empty summary contains nothing`() {
        val summary = Summary.of(emptyList(), random)
        assertTrue(ids(100).none { Summary.mayContain(summary, it) })
    }

    @Test
    fun `two summaries of the same set share neither salt nor bits`() {
        val held = ids(50)
        val a = Summary.of(held, random)
        val b = Summary.of(held, random)
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.filter, b.filter)
    }

    @Test
    fun `a summary goes on the air as one fixed-size frame`() {
        val summary = Summary.of(ids(10), random)
        val bytes = WireCodec.encode(summary)
        assertEquals(240, bytes.size)
        assertEquals(summary, assertIs<DecodeResult.Accepted>(WireCodec.decode(bytes)).frame)
    }
}
