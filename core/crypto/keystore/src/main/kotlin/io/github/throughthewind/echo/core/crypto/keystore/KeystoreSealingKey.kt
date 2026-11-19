package io.github.throughthewind.echo.core.crypto.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.google.crypto.tink.integration.android.AndroidKeystoreAesGcm
import io.github.throughthewind.echo.core.crypto.Sealers
import io.github.throughthewind.echo.core.crypto.SealingKey
import java.security.KeyStore
import javax.crypto.KeyGenerator

/**
 * The key that seals the app's stored data (spec FR-12.1, threat model §5.6): an AES-256-GCM key in
 * the Android Keystore that never leaves it, hardware-backed (StrongBox where the phone has one),
 * and usable only while the phone is unlocked. Encryption is Tink's `AndroidKeystoreAesGcm`, so the
 * ciphertext format is Tink's, not ours.
 *
 * [SealingKey.destroy] deletes the Keystore entry: the crypto-erase step of the panic wipe (WI-024).
 */
object KeystoreSealingKey {
    private const val PROVIDER = "AndroidKeyStore"

    /** Opens the key under [alias], creating it on first use. */
    fun getOrCreate(alias: String): SealingKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(alias)) create(alias)
        return Sealers.of(AndroidKeystoreAesGcm(alias)) { destroy(alias) }
    }

    /** Deletes the key under [alias], if there is one. Everything it sealed is unreadable after. */
    fun destroy(alias: String) {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun create(alias: String) {
        try {
            generate(alias, strongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generate(alias, strongBox = false)
        }
    }

    private fun generate(
        alias: String,
        strongBox: Boolean,
    ) {
        val spec =
            KeyGenParameterSpec
                .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUnlockedDeviceRequired(true)
                .setIsStrongBoxBacked(strongBox)
                .build()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).apply { init(spec) }.generateKey()
    }
}
