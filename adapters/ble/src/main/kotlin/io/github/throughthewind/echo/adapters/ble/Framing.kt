package io.github.throughthewind.echo.adapters.ble

import java.io.IOException

/**
 * Frames over an L2CAP byte stream: a 2-byte big-endian length, then the frame. A length of zero or
 * above [maxFrameBytes] closes the link, without allocating from it (crypto rule: never allocate
 * from a length read off the air).
 */
internal class Framing(
    private val maxFrameBytes: Int,
) {
    fun encode(frame: ByteArray): ByteArray {
        require(frame.size in 1..maxFrameBytes) { "frame size ${frame.size}" }
        return byteArrayOf((frame.size ushr 8).toByte(), frame.size.toByte()) + frame
    }

    /** The next frame from [link], or null at the end of the stream. Throws on a bad length. */
    suspend fun read(link: Link): ByteArray? {
        val header = ByteArray(HEADER)
        if (!readFully(link, header)) return null
        val size = (header[0].toInt() and 0xFF shl 8) or (header[1].toInt() and 0xFF)
        if (size == 0 || size > maxFrameBytes) throw IOException("bad frame length $size")
        val frame = ByteArray(size)
        if (!readFully(link, frame)) throw IOException("stream ended inside a frame")
        return frame
    }

    private suspend fun readFully(
        link: Link,
        buffer: ByteArray,
    ): Boolean {
        var read = 0
        while (read < buffer.size) {
            val chunk = ByteArray(buffer.size - read)
            val n = link.read(chunk)
            if (n < 0) {
                if (read == 0) return false
                throw IOException("stream ended after $read of ${buffer.size} bytes")
            }
            chunk.copyInto(buffer, read, 0, n)
            read += n
        }
        return true
    }

    companion object {
        const val HEADER = 2
    }
}
