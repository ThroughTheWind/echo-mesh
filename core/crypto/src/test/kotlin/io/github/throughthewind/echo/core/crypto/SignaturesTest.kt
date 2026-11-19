package io.github.throughthewind.echo.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SignaturesTest {
    private val message = "a report".encodeToByteArray()

    @Test
    fun `a one-time key signs and the signature verifies over the same bytes only`() {
        val key = OneTimeSigningKey.generate()
        val signature = key.sign(message)
        assertEquals(OneTimeSigningKey.PUBLIC_KEY_BYTES, key.publicKey.size)
        assertEquals(OneTimeSigningKey.SIGNATURE_BYTES, signature.size)
        assertTrue(Signatures.verify(key.publicKey, signature, message))
        assertFalse(Signatures.verify(key.publicKey, signature, message + 0))
        val flipped = signature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertFalse(Signatures.verify(key.publicKey, flipped, message))
    }

    @Test
    fun `another key's signature does not verify`() {
        val a = OneTimeSigningKey.generate()
        val b = OneTimeSigningKey.generate()
        assertFalse(Signatures.verify(b.publicKey, a.sign(message), message))
    }

    @Test
    fun `every key is fresh`() {
        val keys = List(50) { OneTimeSigningKey.generate().publicKey.toList() }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `malformed input off the air is false, never an exception`() {
        val key = OneTimeSigningKey.generate()
        val signature = key.sign(message)
        assertFalse(Signatures.verify(ByteArray(0), signature, message))
        assertFalse(Signatures.verify(ByteArray(31), signature, message))
        assertFalse(Signatures.verify(key.publicKey, signature.copyOf(63), message))
        assertFalse(Signatures.verify(ByteArray(32), ByteArray(64), message))
        assertFalse(Signatures.verify(ByteArray(32) { -1 }, ByteArray(64) { -1 }, message))
    }

    @Test
    fun `the public key getter hands out a copy`() {
        val key = OneTimeSigningKey.generate()
        key.publicKey.fill(0)
        assertNotEquals(ByteArray(32).toList(), key.publicKey.toList())
        assertEquals("OneTimeSigningKey", key.toString())
    }

    @Test
    fun `SHA-256 matches the FIPS 180-2 abc vector`() {
        val expected =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        assertContentEquals(expected, Hashes.sha256("abc".encodeToByteArray()))
    }
}
