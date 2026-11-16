package conventions.gates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportBoundariesTest {
    private val root = "io.github.throughthewind.echo"
    private val registration = setOf("TransportRegistration.kt")

    private fun transport(
        path: String,
        vararg lines: String,
    ) = ImportBoundaries.transportViolations(path, lines.toList(), root, registration)

    @Test
    fun `core and app may not import an adapter`() {
        assertEquals(1, transport("core/mesh/src/main/kotlin/X.kt", "import $root.adapters.ble.BleTransport").size)
        assertEquals(1, transport("app/src/main/kotlin/Y.kt", "import $root.adapters.loopback.*").size)
    }

    @Test
    fun `the registration file and contract imports pass`() {
        assertTrue(transport("app/src/main/kotlin/a/TransportRegistration.kt", "import $root.adapters.ble.BleTransport").isEmpty())
        assertTrue(transport("app/src/main/kotlin/Y.kt", "import $root.core.mesh.MeshTransport").isEmpty())
    }

    @Test
    fun `a registration file name only counts inside app`() {
        assertEquals(1, transport("core/mesh/src/main/kotlin/TransportRegistration.kt", "import $root.adapters.ble.X").size)
    }

    @Test
    fun `an adapter may import itself but not another adapter`() {
        assertTrue(transport("adapters/ble/src/main/kotlin/A.kt", "import $root.adapters.ble.internal.Gatt").isEmpty())
        val violations = transport("adapters/ble/src/main/kotlin/A.kt", "", "import $root.adapters.wifidirect.Group")
        assertEquals(listOf(ImportBoundaries.Violation("adapters/ble/src/main/kotlin/A.kt", 2, "$root.adapters.wifidirect.Group")), violations)
    }

    @Test
    fun `crypto APIs are refused outside core crypto`() {
        val packages = listOf("javax.crypto", "java.security")
        assertEquals(1, ImportBoundaries.cryptoViolations("core/mesh/src/main/kotlin/A.kt", listOf("import javax.crypto.Cipher"), packages).size)
        assertTrue(ImportBoundaries.cryptoViolations("core/crypto/src/main/kotlin/A.kt", listOf("import javax.crypto.Cipher"), packages).isEmpty())
        assertTrue(ImportBoundaries.cryptoViolations("core/mesh/src/main/kotlin/A.kt", listOf("import java.securityx.Foo"), packages).isEmpty())
    }
}
