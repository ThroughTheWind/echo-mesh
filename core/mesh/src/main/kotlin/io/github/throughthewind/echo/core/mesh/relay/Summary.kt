package io.github.throughthewind.echo.core.mesh.relay

import io.github.throughthewind.echo.core.crypto.Hashes
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.SummaryFrame
import io.github.throughthewind.echo.core.mesh.wire.Wire
import kotlin.random.Random

/**
 * A salted Bloom filter over the message ids a phone holds (spec FR-5.1, ADR-0007). A neighbour
 * that receives it pushes back only the frames whose ids are not in it.
 *
 * Each summary has a fresh random salt and sets [HASHES] bits per id, taken from SHA-256 of the salt
 * and the id. A false positive only means a neighbour does not push one frame this round; another
 * neighbour, or the next round, will.
 */
object Summary {
    const val HASHES: Int = 4
    const val BITS: Int = Wire.SUMMARY_FILTER_BYTES * 8

    fun of(
        ids: Collection<Bytes>,
        random: Random,
    ): SummaryFrame {
        val salt = random.nextBytes(Wire.SUMMARY_SALT_BYTES)
        val filter = ByteArray(Wire.SUMMARY_FILTER_BYTES)
        for (id in ids) {
            for (bit in bits(salt, id)) filter[bit ushr 3] = (filter[bit ushr 3].toInt() or (1 shl (bit and 7))).toByte()
        }
        return SummaryFrame(Bytes(salt), Bytes(filter))
    }

    /** False when [id] is certainly not in [summary]; true when it probably is. */
    fun mayContain(
        summary: SummaryFrame,
        id: Bytes,
    ): Boolean {
        val filter = summary.filter.toByteArray()
        return bits(summary.salt.toByteArray(), id).all { bit -> filter[bit ushr 3].toInt() and (1 shl (bit and 7)) != 0 }
    }

    private fun bits(
        salt: ByteArray,
        id: Bytes,
    ): IntArray {
        val h = Hashes.sha256(salt, id.toByteArray())
        return IntArray(HASHES) { j -> (((h[2 * j].toInt() and 0xFF) shl 8) or (h[2 * j + 1].toInt() and 0xFF)) % BITS }
    }
}
