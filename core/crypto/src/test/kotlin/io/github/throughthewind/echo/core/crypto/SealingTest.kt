package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.XChaCha20Poly1305
import java.security.InvalidKeyException
import java.security.ProviderException
import javax.crypto.AEADBadTagException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SealingTest {
    private val text = "settings".encodeToByteArray()
    private val here = "file-a".encodeToByteArray()

    @Test
    fun `a sealed text opens only with its key and its context`() {
        val key = Sealers.ephemeral()
        val sealed = key.seal(text, here)
        assertContentEquals(text, key.open(sealed, here))
        assertNull(key.open(sealed, "file-b".encodeToByteArray()))
        assertNull(Sealers.ephemeral().open(sealed, here))
        assertNull(key.open(sealed.copyOf(sealed.size - 1), here))
        assertNull(key.open(ByteArray(0), here))
    }

    @Test
    fun `a destroyed key opens nothing and seals nothing`() {
        val key = Sealers.ephemeral()
        val sealed = key.seal(text, here)
        key.destroy()
        assertNull(key.open(sealed, here))
        assertFailsWith<IllegalStateException> { key.seal(text, here) }
    }

    @Test
    fun `a key over another Tink AEAD runs its own destroy`() {
        var destroyed = false
        val key = Sealers.of(XChaCha20Poly1305(ByteArray(32))) { destroyed = true }
        key.destroy()
        assert(destroyed)
    }

    /** An AEAD that fails as the Android Keystore does while the phone is locked ("Device locked"). */
    private class LockedAead(
        private val failure: () -> Exception,
    ) : Aead {
        override fun encrypt(
            plaintext: ByteArray,
            associatedData: ByteArray,
        ): ByteArray = throw failure()

        override fun decrypt(
            ciphertext: ByteArray,
            associatedData: ByteArray,
        ): ByteArray = throw failure()
    }

    @Test
    fun `a key the platform refuses says so, instead of crashing or reading as tampered`() {
        for (failure in listOf({ InvalidKeyException("Keystore operation failed") }, { ProviderException("Device locked") })) {
            val key = Sealers.of(LockedAead(failure)) {}
            assertFailsWith<SealingUnavailableException> { key.seal(text, here) }
            assertFailsWith<SealingUnavailableException> { key.open(ByteArray(40), here) }
        }
        // A file that fails its tag is tampered, not locked: null, as before.
        assertNull(Sealers.of(LockedAead { AEADBadTagException("tag") }) {}.open(ByteArray(40), here))
    }
}
