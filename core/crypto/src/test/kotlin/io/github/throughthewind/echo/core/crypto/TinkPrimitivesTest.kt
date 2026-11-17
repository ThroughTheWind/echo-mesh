package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.Random
import com.google.crypto.tink.subtle.X25519
import com.google.crypto.tink.subtle.XChaCha20Poly1305
import java.security.GeneralSecurityException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * ADR-0004 rests on one artifact, tink-android, giving every primitive the design uses with the same
 * behaviour on the JVM, where the mesh is tested, as on Android. This proves the JVM half; the
 * protocol code that uses them comes with WI-008, WI-017 and WI-018.
 */
class TinkPrimitivesTest {
    private val message = "a report".encodeToByteArray()

    @Test
    fun `Ed25519 signs and verifies, and rejects a changed message`() {
        val keys = Ed25519Sign.KeyPair.newKeyPair()
        assertEquals(32, keys.publicKey.size)
        val signature = Ed25519Sign(keys.privateKey).sign(message)
        assertEquals(64, signature.size)
        Ed25519Verify(keys.publicKey).verify(signature, message)
        assertFailsWith<GeneralSecurityException> { Ed25519Verify(keys.publicKey).verify(signature, message + 0) }
    }

    @Test
    fun `XChaCha20-Poly1305 round-trips with a random 24-byte nonce and rejects tampering`() {
        val aead = XChaCha20Poly1305(Random.randBytes(32))
        val associated = byteArrayOf(1, 4)
        val sealed = aead.encrypt(message, associated)
        // Nonce, ciphertext, tag: the overhead the wire format's group frame budgets for (ADR-0005).
        assertEquals(24 + message.size + 16, sealed.size)
        assertContentEquals(message, aead.decrypt(sealed, associated))
        sealed[30] = (sealed[30].toInt() xor 1).toByte()
        assertFailsWith<GeneralSecurityException> { aead.decrypt(sealed, associated) }
    }

    @Test
    fun `X25519 agrees on one secret from both sides`() {
        val a = X25519.generatePrivateKey()
        val b = X25519.generatePrivateKey()
        assertContentEquals(
            X25519.computeSharedSecret(a, X25519.publicFromPrivate(b)),
            X25519.computeSharedSecret(b, X25519.publicFromPrivate(a)),
        )
    }

    @Test
    fun `HKDF-SHA256 matches RFC 5869 test case 1`() {
        val ikm = ByteArray(22) { 0x0b }
        val salt = ByteArray(13) { it.toByte() }
        val info = ByteArray(10) { (0xf0 + it).toByte() }
        val okm = Hkdf.computeHkdf("HMACSHA256", ikm, salt, info, 42)
        val expected =
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        assertContentEquals(expected, okm)
    }
}
