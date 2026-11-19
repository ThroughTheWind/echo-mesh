package io.github.throughthewind.echo.adapters.ble

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Nothing stable on the air (spec FR-3.4, threat model §5.1), checked in the source of the only file
 * that talks to Android's advertiser. Android puts the device name in an advertisement only when
 * asked, so every builder must say false, and nothing else that names the phone may be added.
 */
class AdvertisementPrivacyTest {
    private val source =
        File("src/main/kotlin/io/github/throughthewind/echo/adapters/ble/AndroidBleRadio.kt").readText()

    @Test
    fun `every advertisement builder leaves the device name out`() {
        val builders = Regex("""AdvertiseData\s*\.Builder\(\)""").findAll(source).count()
        val nameless = Regex("""setIncludeDeviceName\(false\)""").findAll(source).count()
        assertTrue(builders > 0, "no advertisement builder found: did the file move?")
        assertEquals(builders, nameless, "each AdvertiseData.Builder needs setIncludeDeviceName(false)")
    }

    @Test
    fun `nothing that names the phone is ever advertised`() {
        listOf(
            "setIncludeDeviceName(true)",
            "setName(",
            "addManufacturerData",
            "setIncludeTxPowerLevel(true)",
            "getAddress",
            ".address",
            "setDeviceName",
        ).forEach { forbidden -> assertFalse(forbidden in source, "AndroidBleRadio.kt must not use $forbidden") }
    }

    @Test
    fun `no GATT server and no pairing`() {
        listOf("openGattServer", "createBond", "createRfcomm", "listenUsingL2capChannel(", "createL2capChannel(").forEach {
            assertFalse(it in source, "AndroidBleRadio.kt must not use $it")
        }
    }
}
