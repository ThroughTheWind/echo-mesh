package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.Random
import com.google.crypto.tink.subtle.XChaCha20Poly1305
import java.security.GeneralSecurityException

/**
 * Authenticated encryption for what the app stores (spec FR-12.1, threat model §5.6). [context]
 * binds a ciphertext to where it belongs, such as a file name, so one file cannot be swapped for
 * another. The format is Tink's, never ours.
 */
interface Sealer {
    fun seal(
        plaintext: ByteArray,
        context: ByteArray,
    ): ByteArray

    /** The plaintext, or null when [sealed] was not sealed by this key for [context], or the key is gone. */
    fun open(
        sealed: ByteArray,
        context: ByteArray,
    ): ByteArray?
}

/**
 * A [Sealer] whose key can be destroyed. Destroying it is the crypto-erase of the panic wipe
 * (WI-024): every file it sealed becomes unreadable at once, before any file is deleted.
 */
interface SealingKey : Sealer {
    /** Destroys the key. Afterwards [open] returns null and [seal] throws. Cannot be undone. */
    fun destroy()
}

object Sealers {
    /** A [SealingKey] over a Tink [aead], such as an Android Keystore key; [destroy] is the key's own. */
    fun of(
        aead: Aead,
        destroy: () -> Unit,
    ): SealingKey = TinkSealingKey(aead, destroy)

    /**
     * A key that lives in memory only and dies with the process. For tests, and for anything that
     * must not outlive the app run.
     */
    fun ephemeral(): SealingKey {
        val key = Random.randBytes(32)
        val aead = XChaCha20Poly1305(key)
        return TinkSealingKey(aead) { key.fill(0) }
    }
}

private class TinkSealingKey(
    private val aead: Aead,
    private val onDestroy: () -> Unit,
) : SealingKey {
    @Volatile private var destroyed = false

    override fun seal(
        plaintext: ByteArray,
        context: ByteArray,
    ): ByteArray {
        check(!destroyed) { "the key was destroyed" }
        return aead.encrypt(plaintext, context)
    }

    override fun open(
        sealed: ByteArray,
        context: ByteArray,
    ): ByteArray? {
        if (destroyed) return null
        return try {
            aead.decrypt(sealed, context)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    override fun destroy() {
        destroyed = true
        onDestroy()
    }

    override fun toString() = "SealingKey"
}
