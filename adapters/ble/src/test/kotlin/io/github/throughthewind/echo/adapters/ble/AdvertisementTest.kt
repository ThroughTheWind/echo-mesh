package io.github.throughthewind.echo.adapters.ble

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdvertisementTest {
    @Test
    fun `an advertisement survives the round trip`() {
        val random = Random(7)
        repeat(100) {
            val ad = Advertisement(random.nextInt(), random.nextInt(1, 0x10000))
            assertEquals(ad, Advertisement.decode(ad.encode()))
        }
    }

    @Test
    fun `it is six bytes, small enough for a legacy scan response`() {
        // A legacy scan response holds 31 bytes; service data with a 128-bit UUID takes 18 of them before the payload.
        assertEquals(Advertisement.SIZE, Advertisement(-1, 0xFFFF).encode().size)
    }

    @Test
    fun `anything else on the air is ignored`() {
        assertNull(Advertisement.decode(ByteArray(0)))
        assertNull(Advertisement.decode(ByteArray(5)))
        assertNull(Advertisement.decode(ByteArray(7)))
        assertNull(Advertisement.decode(byteArrayOf(1, 2, 3, 4, 0, 0)))
    }

    @Test
    fun `the hello carries the token`() {
        assertEquals(0x12345678, Advertisement.tokenOf(Advertisement.tokenBytes(0x12345678)))
        assertEquals(-2, Advertisement.tokenOf(Advertisement.tokenBytes(-2)))
    }
}
