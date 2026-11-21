package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.aead.internal.InsecureNonceChaCha20Poly1305
import com.google.crypto.tink.subtle.X25519
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The join handshake (ADR-0004): `Noise_NKpsk0_25519_ChaChaPoly_SHA256`, Noise Protocol Framework
 * revision 34, on Tink's X25519 and ChaCha20-Poly1305 and the JDK's SHA-256 and HMAC.
 *
 * ```
 * NKpsk0:
 *   <- s
 *   ...
 *   -> psk, e, es
 *   <- e, ee
 * ```
 *
 * The responder (the member showing the QR code) has a one-time static key whose public half and
 * the PSK are in the code. The initiator (the newcomer who scanned it) proves it saw the code; the
 * responder's reply is encrypted to keys only the two of them hold, forward-secret from the first
 * byte. Checked against the published cacophony test vectors (`NoiseVectorsTest`) before anything
 * uses it. Not thread-safe: one handshake belongs to one coroutine.
 */
class NoiseHandshake internal constructor(
    private val initiator: Boolean,
    prologue: ByteArray,
    private val psk: ByteArray,
    private val localStatic: ByteArray?,
    private var remoteStatic: ByteArray?,
    private val localEphemeral: ByteArray,
) {
    private val symmetric = SymmetricState(PROTOCOL_NAME.encodeToByteArray())
    private var remoteEphemeral: ByteArray? = null
    private var step = 0
    private var failed = false

    init {
        require(psk.size == KEY_BYTES) { "the PSK is 32 bytes" }
        symmetric.mixHash(prologue)
        // Pre-message pattern "<- s": both sides hash the responder's static public key.
        val responderStatic = if (initiator) remoteStatic else localStatic?.let { X25519.publicFromPrivate(it) }
        symmetric.mixHash(requireNotNull(responderStatic) { "NK needs the responder's static key" })
    }

    /** True once both handshake messages have passed; then [split] gives the session keys. */
    val isComplete: Boolean get() = step == 2 && !failed

    /** The handshake hash, the same on both sides once complete (channel binding). */
    val handshakeHash: ByteArray get() = symmetric.h.copyOf()

    /** The next handshake message, carrying [payload] (encrypted on both messages, thanks to the PSK). */
    fun writeMessage(payload: ByteArray): ByteArray {
        check(!failed && step < 2 && (step == 0) == initiator) { "not this side's turn to write" }
        val out = mutableListOf<ByteArray>()
        val ephemeralPublic = X25519.publicFromPrivate(localEphemeral)
        if (step == 0) {
            symmetric.mixKeyAndHash(psk)
            out += ephemeralPublic
            symmetric.mixHash(ephemeralPublic)
            symmetric.mixKey(ephemeralPublic)
            symmetric.mixKey(dh(localEphemeral, remoteStatic!!))
        } else {
            out += ephemeralPublic
            symmetric.mixHash(ephemeralPublic)
            symmetric.mixKey(ephemeralPublic)
            symmetric.mixKey(dh(localEphemeral, remoteEphemeral!!))
        }
        out += symmetric.encryptAndHash(payload)
        step++
        return out.fold(ByteArray(0)) { acc, part -> acc + part }
    }

    /**
     * The payload of the next handshake message, or null if it is malformed, tampered with, or for
     * another key. After a null the handshake is dead: a newcomer scans again (threat model §5.5).
     */
    fun readMessage(message: ByteArray): ByteArray? {
        if (failed || step >= 2 || (step == 0) != !initiator) return null
        if (message.size < KEY_BYTES + TAG_BYTES || message.size > MAX_MESSAGE_BYTES) return fail()
        val ephemeral = message.copyOfRange(0, KEY_BYTES)
        val ciphertext = message.copyOfRange(KEY_BYTES, message.size)
        return try {
            if (step == 0) {
                symmetric.mixKeyAndHash(psk)
                remoteEphemeral = ephemeral
                symmetric.mixHash(ephemeral)
                symmetric.mixKey(ephemeral)
                symmetric.mixKey(dh(localStatic!!, ephemeral))
            } else {
                remoteEphemeral = ephemeral
                symmetric.mixHash(ephemeral)
                symmetric.mixKey(ephemeral)
                symmetric.mixKey(dh(localEphemeral, ephemeral))
            }
            val payload = symmetric.decryptAndHash(ciphertext) ?: return fail()
            step++
            payload
        } catch (_: GeneralSecurityException) {
            fail()
        }
    }

    /** The two transport keys, sending first. Only after the handshake is complete. */
    fun split(): NoiseSession {
        check(isComplete) { "the handshake is not complete" }
        val (toResponder, toInitiator) = symmetric.split()
        return if (initiator) NoiseSession(toResponder, toInitiator) else NoiseSession(toInitiator, toResponder)
    }

    private fun fail(): ByteArray? {
        failed = true
        return null
    }

    override fun toString() = "NoiseHandshake"

    companion object {
        const val PROTOCOL_NAME = "Noise_NKpsk0_25519_ChaChaPoly_SHA256"
        const val KEY_BYTES = 32
        const val TAG_BYTES = 16

        /** Noise's own limit on any message. */
        const val MAX_MESSAGE_BYTES = 65_535

        /** The newcomer's side: it scanned [responderStatic] and [psk] from the QR code. */
        fun initiator(
            responderStatic: ByteArray,
            psk: ByteArray,
            prologue: ByteArray,
        ): NoiseHandshake {
            require(responderStatic.size == KEY_BYTES) { "an X25519 public key is 32 bytes" }
            return NoiseHandshake(true, prologue, psk, null, responderStatic.copyOf(), X25519.generatePrivateKey())
        }

        /** The member's side: it shows the public half of [static] and [psk] in the QR code. */
        fun responder(
            static: NoiseStaticKey,
            psk: ByteArray,
            prologue: ByteArray,
        ): NoiseHandshake = NoiseHandshake(false, prologue, psk, static.privateKey, null, X25519.generatePrivateKey())

        private fun dh(
            private: ByteArray,
            public: ByteArray,
        ): ByteArray =
            try {
                X25519.computeSharedSecret(private, public)
            } catch (e: InvalidKeyException) {
                // A low-order point from the air: refuse it, as an all-zero DH output would be.
                throw GeneralSecurityException(e)
            }
    }
}

/** A one-time X25519 key for one QR code: shown once, used for one join, then dropped. */
class NoiseStaticKey private constructor(
    internal val privateKey: ByteArray,
) {
    val publicKey: ByteArray get() = X25519.publicFromPrivate(privateKey)

    /** Overwrites the private key; the code it was shown in can no longer be used. */
    fun destroy() = privateKey.fill(0)

    override fun toString() = "NoiseStaticKey"

    companion object {
        fun generate(): NoiseStaticKey = NoiseStaticKey(X25519.generatePrivateKey())

        internal fun of(privateKey: ByteArray) = NoiseStaticKey(privateKey.copyOf())
    }
}

/** The two directions after a handshake. Each message is ChaCha20-Poly1305 with a counter nonce. */
class NoiseSession internal constructor(
    private val sending: CipherState,
    private val receiving: CipherState,
) {
    fun encrypt(plaintext: ByteArray): ByteArray = sending.encryptWithAd(ByteArray(0), plaintext)

    /** Null when [ciphertext] does not open: tampered, replayed, reordered or for another session. */
    fun decrypt(ciphertext: ByteArray): ByteArray? =
        try {
            receiving.decryptWithAd(ByteArray(0), ciphertext)
        } catch (_: GeneralSecurityException) {
            null
        }

    override fun toString() = "NoiseSession"
}

/** Noise §5.1. */
internal class CipherState {
    private var k: ByteArray? = null
    private var n = 0L

    val hasKey: Boolean get() = k != null

    fun initializeKey(key: ByteArray) {
        k = key
        n = 0
    }

    fun encryptWithAd(
        ad: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        val key = k ?: return plaintext
        val out = InsecureNonceChaCha20Poly1305(key).encrypt(nonce(n), plaintext, ad)
        n++
        return out
    }

    /** Throws on a bad tag; the nonce moves on only after a message that opened. */
    fun decryptWithAd(
        ad: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        val key = k ?: return ciphertext
        val out = InsecureNonceChaCha20Poly1305(key).decrypt(nonce(n), ciphertext, ad)
        n++
        return out
    }

    /** 32 zero bits, then the counter as 64-bit little-endian (Noise §12.3). */
    private fun nonce(counter: Long): ByteArray = ByteArray(12) { i -> if (i < 4) 0 else (counter ushr (8 * (i - 4))).toByte() }
}

/** Noise §5.2. */
internal class SymmetricState(
    protocolName: ByteArray,
) {
    private val cipher = CipherState()
    var h: ByteArray =
        if (protocolName.size <= HASH_BYTES) protocolName.copyOf(HASH_BYTES) else sha256(protocolName)
        private set
    private var ck: ByteArray = h.copyOf()

    fun mixKey(ikm: ByteArray) {
        val (nextCk, tempK) = hkdf(ck, ikm, 2)
        ck = nextCk
        cipher.initializeKey(tempK)
    }

    fun mixHash(data: ByteArray) {
        h = sha256(h + data)
    }

    fun mixKeyAndHash(ikm: ByteArray) {
        val (nextCk, tempH, tempK) = hkdf(ck, ikm, 3)
        ck = nextCk
        mixHash(tempH)
        cipher.initializeKey(tempK)
    }

    fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val ciphertext = cipher.encryptWithAd(h, plaintext)
        mixHash(ciphertext)
        return ciphertext
    }

    /** Null when the tag does not verify. */
    fun decryptAndHash(ciphertext: ByteArray): ByteArray? {
        if (cipher.hasKey && ciphertext.size < NoiseHandshake.TAG_BYTES) return null
        val plaintext =
            try {
                cipher.decryptWithAd(h, ciphertext)
            } catch (_: GeneralSecurityException) {
                return null
            }
        mixHash(ciphertext)
        return plaintext
    }

    fun split(): Pair<CipherState, CipherState> {
        val (k1, k2) = hkdf(ck, ByteArray(0), 2)
        return CipherState().apply { initializeKey(k1) } to CipherState().apply { initializeKey(k2) }
    }

    private companion object {
        const val HASH_BYTES = 32

        fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

        fun hmac(
            key: ByteArray,
            data: ByteArray,
        ): ByteArray = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

        /** Noise §4.3: HKDF with the chaining key as salt, two or three outputs. */
        fun hkdf(
            chainingKey: ByteArray,
            ikm: ByteArray,
            outputs: Int,
        ): List<ByteArray> {
            val temp = hmac(chainingKey, ikm)
            val out1 = hmac(temp, byteArrayOf(1))
            val out2 = hmac(temp, out1 + byteArrayOf(2))
            if (outputs == 2) return listOf(out1, out2)
            val out3 = hmac(temp, out2 + byteArrayOf(3))
            return listOf(out1, out2, out3)
        }
    }
}
