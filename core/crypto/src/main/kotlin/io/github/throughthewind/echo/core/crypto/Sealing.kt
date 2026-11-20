package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.Random
import com.google.crypto.tink.subtle.XChaCha20Poly1305
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.ProviderException

/**
 * Authenticated encryption for what the app stores (spec FR-12.1, threat model §5.6). [context]
 * binds a ciphertext to where it belongs, such as a file name, so one file cannot be swapped for
 * another. The format is Tink's, never ours.
 */
interface Sealer {
    /** [plaintext] sealed for [context]; throws [SealingUnavailableException] while the key cannot be used. */
    fun seal(
        plaintext: ByteArray,
        context: ByteArray,
    ): ByteArray

    /**
     * The plaintext, or null when [sealed] was not sealed by this key for [context], or the key is gone.
     * Throws [SealingUnavailableException] while the key exists but cannot be used, so a caller never
     * mistakes a locked phone for a missing or tampered file.
     */
    fun open(
        sealed: ByteArray,
        context: ByteArray,
    ): ByteArray?
}

/**
 * The key exists but the platform will not use it now: an Android Keystore key made
 * `setUnlockedDeviceRequired` refuses every operation while the phone is locked ("Device locked",
 * found on a Pixel 9a, WI-077). Nothing is lost; the caller keeps the data in memory and tries again
 * once the phone is unlocked. An [IOException], so the stores that already retry a failed write do so.
 */
class SealingUnavailableException(
    cause: Throwable,
) : IOException("the sealing key cannot be used now", cause)

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
        return try {
            aead.encrypt(plaintext, context)
        } catch (e: GeneralSecurityException) {
            // Sealing fresh bytes fails only when the key cannot be used, never because of the input.
            throw SealingUnavailableException(e)
        } catch (e: ProviderException) {
            throw SealingUnavailableException(e)
        }
    }

    override fun open(
        sealed: ByteArray,
        context: ByteArray,
    ): ByteArray? {
        if (destroyed) return null
        return try {
            aead.decrypt(sealed, context)
        } catch (e: InvalidKeyException) {
            // The key refused to start (locked phone), which says nothing about the file.
            throw SealingUnavailableException(e)
        } catch (e: ProviderException) {
            throw SealingUnavailableException(e)
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
