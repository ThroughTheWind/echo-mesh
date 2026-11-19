package io.github.throughthewind.echo.adapters.ble

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FramingTest {
    private val framing = Framing(maxFrameBytes = 256)

    /** A link that delivers [chunks] one per read, then ends. */
    private fun linkOf(vararg chunks: ByteArray): Link {
        val inbound = Channel<ByteArray>(Channel.UNLIMITED)
        chunks.forEach { inbound.trySend(it) }
        inbound.close()
        return FakeLink(inbound = inbound, outbound = Channel(Channel.UNLIMITED))
    }

    @Test
    fun `a frame survives the round trip, even split across reads`() =
        runTest {
            val frame = ByteArray(200) { it.toByte() }
            val bytes = framing.encode(frame)
            val link = linkOf(bytes.copyOfRange(0, 1), bytes.copyOfRange(1, 50), bytes.copyOfRange(50, bytes.size))
            assertContentEquals(frame, framing.read(link))
            assertNull(framing.read(link))
        }

    @Test
    fun `two frames in one read come out as two`() =
        runTest {
            val link = linkOf(framing.encode(byteArrayOf(1)) + framing.encode(byteArrayOf(2, 3)))
            assertContentEquals(byteArrayOf(1), framing.read(link))
            assertContentEquals(byteArrayOf(2, 3), framing.read(link))
        }

    @Test
    fun `a length above the maximum closes the link without allocating it`() =
        runTest {
            assertFailsWith<IOException> { framing.read(linkOf(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))) }
            assertFailsWith<IOException> { framing.read(linkOf(byteArrayOf(0x01, 0x01))) }
        }

    @Test
    fun `a zero length is refused`() =
        runTest {
            assertFailsWith<IOException> { framing.read(linkOf(byteArrayOf(0, 0))) }
        }

    @Test
    fun `a stream that ends inside a frame is an error, not a short frame`() =
        runTest {
            assertFailsWith<IOException> { framing.read(linkOf(byteArrayOf(0, 5, 1, 2))) }
            assertFailsWith<IOException> { framing.read(linkOf(byteArrayOf(0))) }
        }

    @Test
    fun `the sender refuses an empty or oversized frame`() {
        assertFailsWith<IllegalArgumentException> { framing.encode(ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { framing.encode(ByteArray(257)) }
    }
}
