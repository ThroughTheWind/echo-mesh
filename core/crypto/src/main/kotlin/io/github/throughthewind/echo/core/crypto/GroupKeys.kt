package io.github.throughthewind.echo.core.crypto

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.XChaCha20Poly1305
import java.security.GeneralSecurityException

/**
 * A group's epoch keys (ADR-0004, ADR-0009, threat model §5.3): one key per UTC hour, each derived
 * from the one before by a one-way HKDF ratchet. Holds the current epoch and at most [KEPT_BEHIND]
 * before it; [advanceTo] deletes older keys, so a phone seized now cannot read traffic from more
 * than a few hours ago. Never logged, and stored only sealed (`core/data`).
 */
class GroupEpochs private constructor(
    oldestEpoch: Long,
    oldestKey: ByteArray,
) {
    /** The epochs held, oldest first; the last is the newest this phone has reached. */
    private val keys = ArrayDeque<Pair<Long, ByteArray>>().apply { add(oldestEpoch to oldestKey) }

    /** The newest epoch held. */
    val newest: Long get() = keys.last().first

    /** The oldest epoch whose key is still held. */
    val oldest: Long get() = keys.first().first

    /**
     * The key for [epoch]: held, or ratcheted forward from the newest held key without keeping the
     * steps. Null for an epoch older than every key held: that key is gone for good.
     */
    fun keyFor(epoch: Long): ByteArray? {
        keys.firstOrNull { it.first == epoch }?.let { return it.second.copyOf() }
        if (epoch < newest || epoch - newest > MAX_AHEAD) return null
        var key = keys.last().second
        repeat((epoch - newest).toInt()) { key = ratchet(key) }
        return key
    }

    /** Ratchets up to [epoch] and deletes every key older than `epoch - KEPT_BEHIND`. */
    fun advanceTo(epoch: Long) {
        while (newest < epoch) keys.addLast(newest + 1 to ratchet(keys.last().second))
        while (keys.size > 1 && keys.first().first < epoch - KEPT_BEHIND) keys.removeFirst().second.fill(0)
    }

    /** The epochs held, oldest first, for sealed storage. */
    fun export(): List<Pair<Long, ByteArray>> = keys.map { it.first to it.second.copyOf() }

    /** Overwrites every key held (leave or expiry, FR-8.5, FR-8.6). */
    fun destroy() {
        keys.forEach { it.second.fill(0) }
        keys.clear()
    }

    override fun toString() = "GroupEpochs"

    companion object {
        const val KEY_BYTES = 32

        /** The longest report life is 3 hours, so 4 hours back covers every live frame. */
        const val KEPT_BEHIND = 4

        /** A frame from more than this many hours ahead is a clock problem, not a key to derive. */
        const val MAX_AHEAD = 2

        private const val RATCHET_INFO = "echo group ratchet"

        /** A new group's keys, starting at [epoch] (FR-8.1). */
        fun create(epoch: Long): GroupEpochs = GroupEpochs(epoch, Randomness.bytes(KEY_BYTES))

        /** From one key: a joiner gets only the current epoch's (threat model §5.3). */
        fun fromKey(
            epoch: Long,
            key: ByteArray,
        ): GroupEpochs {
            require(key.size == KEY_BYTES) { "an epoch key is $KEY_BYTES bytes" }
            return GroupEpochs(epoch, key.copyOf())
        }

        /** From [export]ed keys, oldest first. */
        fun restore(keys: List<Pair<Long, ByteArray>>): GroupEpochs {
            require(keys.isNotEmpty()) { "no keys" }
            val epochs = fromKey(keys.first().first, keys.first().second)
            keys.drop(1).forEach { (epoch, key) -> epochs.keys.addLast(epoch to key.copyOf()) }
            return epochs
        }

        internal fun ratchet(key: ByteArray): ByteArray =
            Hkdf.computeHkdf("HMACSHA256", key, ByteArray(0), RATCHET_INFO.encodeToByteArray(), KEY_BYTES)
    }
}

/**
 * Seals and opens group frame contents under an epoch key (ADR-0009): XChaCha20-Poly1305 with a
 * random 24-byte nonce under a key derived from the epoch key. The caller gives fixed-size inner
 * messages, so every sealed frame has one size.
 */
object GroupCipher {
    const val NONCE_BYTES = 24
    const val TAG_BYTES = 16
    private const val FRAME_INFO = "echo group frame"

    /** `nonce ‖ ciphertext ‖ tag`. */
    fun seal(
        epochKey: ByteArray,
        inner: ByteArray,
        associatedData: ByteArray,
    ): ByteArray = XChaCha20Poly1305(frameKey(epochKey)).encrypt(inner, associatedData)

    /** The inner message, or null when [sealed] does not open under [epochKey]: the cheap, common case. */
    fun open(
        epochKey: ByteArray,
        sealed: ByteArray,
        associatedData: ByteArray,
    ): ByteArray? {
        if (sealed.size < NONCE_BYTES + TAG_BYTES) return null
        return try {
            XChaCha20Poly1305(frameKey(epochKey)).decrypt(sealed, associatedData)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private fun frameKey(epochKey: ByteArray): ByteArray =
        Hkdf.computeHkdf("HMACSHA256", epochKey, ByteArray(0), FRAME_INFO.encodeToByteArray(), GroupEpochs.KEY_BYTES)
}

/**
 * This phone's member key in one group (threat model §5.3): an Ed25519 key that signs everything it
 * sends to that group, so members know who sent it. One per group, never reused across groups, kept
 * sealed for the group's life and destroyed with it.
 */
class MemberKey private constructor(
    private val seed: ByteArray,
) {
    private val signer = Ed25519Sign(seed)

    val publicKey: ByteArray = Ed25519Sign.KeyPair.newKeyPairFromSeed(seed).publicKey

    fun sign(message: ByteArray): ByteArray = signer.sign(message)

    /** The private seed, only for sealed storage. */
    fun exportSeed(): ByteArray = seed.copyOf()

    fun destroy() = seed.fill(0)

    override fun toString() = "MemberKey"

    companion object {
        const val SEED_BYTES = 32

        fun generate(): MemberKey = MemberKey(Randomness.bytes(SEED_BYTES))

        fun fromSeed(seed: ByteArray): MemberKey {
            require(seed.size == SEED_BYTES) { "a seed is $SEED_BYTES bytes" }
            return MemberKey(seed.copyOf())
        }
    }
}
