package conventions.gates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ManifestPermissionsTest {
    private fun manifest(vararg permissions: String) =
        """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="x">
            ${permissions.joinToString("\n") { "<uses-permission android:name=\"$it\" />" }}
            <uses-permission-sdk-23 android:name="android.permission.ACCESS_FINE_LOCATION" />
            <application />
        </manifest>
        """.trimIndent()

    private val forbidden = setOf("android.permission.INTERNET")

    @Test
    fun `INTERNET in a merged manifest is a violation`() {
        val declared = ManifestPermissions.declared(manifest("android.permission.INTERNET", "android.permission.BLUETOOTH_SCAN"))
        assertEquals(setOf("android.permission.INTERNET"), ManifestPermissions.violations(declared, forbidden, emptySet()))
    }

    @Test
    fun `a manifest without INTERNET passes`() {
        val declared = ManifestPermissions.declared(manifest("android.permission.BLUETOOTH_SCAN"))
        assertTrue("android.permission.ACCESS_FINE_LOCATION" in declared, "uses-permission-sdk-23 is read too")
        assertTrue(ManifestPermissions.violations(declared, forbidden, emptySet()).isEmpty())
    }

    @Test
    fun `an allowed permission passes`() {
        val declared = ManifestPermissions.declared(manifest("android.permission.INTERNET"))
        assertTrue(ManifestPermissions.violations(declared, forbidden, forbidden).isEmpty())
    }

    @Test
    fun `something that is not a manifest fails loudly`() {
        assertFails { ManifestPermissions.declared("<resources />") }
        assertFails { ManifestPermissions.declared("not xml") }
        assertFails { ManifestPermissions.declared("<!DOCTYPE manifest [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><manifest>&x;</manifest>") }
    }
}
