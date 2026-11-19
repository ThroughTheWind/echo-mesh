package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.GeneralSecurityException
import java.security.MessageDigest

/**
 * A fresh Ed25519 key that signs one report, vote or withdrawal and nothing else (threat model §5.2,
 * ADR-0004), so two messages from one phone share no key. It lives in memory only: the author keeps
 * it until the report expires, to sign a withdrawal, and never stores or logs it.
 */
class OneTimeSigningKey private constructor(
    private val signer: Ed25519Sign,
    private val public: ByteArray,
) {
    /** The 32-byte public key that goes on the air next to the signature. */
    val publicKey: ByteArray get() = public.copyOf()

    /** A 64-byte Ed25519 signature over [message]. */
    fun sign(message: ByteArray): ByteArray = signer.sign(message)

    /** Never prints the key: a key does not belong in a log (crypto rule). */
    override fun toString() = "OneTimeSigningKey"

    companion object {
        const val PUBLIC_KEY_BYTES: Int = 32
        const val SIGNATURE_BYTES: Int = 64

        fun generate(): OneTimeSigningKey {
            val pair = Ed25519Sign.KeyPair.newKeyPair()
            val seed = pair.privateKey
            val key = OneTimeSigningKey(Ed25519Sign(seed), pair.publicKey)
            seed.fill(0)
            return key
        }
    }
}

object Signatures {
    /**
     * True when [signature] is a valid Ed25519 signature by [publicKey] over exactly [message]. Any
     * malformed input is a plain `false`, never an exception, because all three come off the air.
     */
    fun verify(
        publicKey: ByteArray,
        signature: ByteArray,
        message: ByteArray,
    ): Boolean {
        if (publicKey.size != OneTimeSigningKey.PUBLIC_KEY_BYTES) return false
        if (signature.size != OneTimeSigningKey.SIGNATURE_BYTES) return false
        return try {
            Ed25519Verify(publicKey).verify(signature, message)
            true
        } catch (_: GeneralSecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}

object Hashes {
    const val SHA256_BYTES: Int = 32

    /** SHA-256 of [bytes]: the message id of a frame (spec FR-5.2, ADR-0005). */
    fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    /** SHA-256 of [parts] one after another, without copying them into one array first. */
    fun sha256(vararg parts: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        parts.forEach(digest::update)
        return digest.digest()
    }
}
