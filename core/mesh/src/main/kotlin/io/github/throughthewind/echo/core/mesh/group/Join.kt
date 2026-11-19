package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.crypto.GroupEpochs
import io.github.throughthewind.echo.core.crypto.Hashes
import io.github.throughthewind.echo.core.crypto.NoiseHandshake
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Wire
import java.nio.ByteBuffer
import java.util.Base64

/**
 * What a member's QR code holds to let one newcomer in (ADR-0009): the one-time Noise static public
 * key and a random token. Shown, scanned once, then useless (AC-25).
 */
class JoinOffer(
    val responderKey: Bytes,
    val token: Bytes,
) {
    init {
        require(responderKey.size == Wire.PUBLIC_KEY_BYTES && token.size == TOKEN_BYTES) { "not a join offer" }
    }

    /** The text the QR code carries: `echo-join:1:` and base64url of key ‖ token. */
    fun encode(): String = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(responderKey.toByteArray() + token.toByteArray())

    /** The Noise PSK: SHA-256 of `"echo join psk" ‖ token`. */
    internal fun psk(): ByteArray = Hashes.sha256(PSK_LABEL, token.toByteArray())

    override fun equals(other: Any?) = other is JoinOffer && other.responderKey == responderKey && other.token == token

    override fun hashCode() = 31 * responderKey.hashCode() + token.hashCode()

    /** Never prints the token: whoever has it can join. */
    override fun toString() = "JoinOffer"

    companion object {
        const val PREFIX = "echo-join:1:"
        const val TOKEN_BYTES = 16
        private const val ENCODED_BYTES = Wire.PUBLIC_KEY_BYTES + TOKEN_BYTES
        private val PSK_LABEL = "echo join psk".encodeToByteArray()

        /** The offer in a scanned code, or null for anything that is not exactly one. */
        fun decode(text: String): JoinOffer? {
            if (!text.startsWith(PREFIX)) return null
            val body = text.substring(PREFIX.length)
            // 48 bytes are exactly 64 base64 characters, so there is one spelling and no padding.
            if (body.length != 64 || body.any { it !in ALPHABET }) return null
            val bytes =
                try {
                    Base64.getUrlDecoder().decode(body)
                } catch (_: IllegalArgumentException) {
                    return null
                }
            if (bytes.size != ENCODED_BYTES) return null
            return JoinOffer(Bytes(bytes.copyOf(Wire.PUBLIC_KEY_BYTES)), Bytes(bytes.copyOfRange(Wire.PUBLIC_KEY_BYTES, ENCODED_BYTES)))
        }

        private val ALPHABET = ('A'..'Z') + ('a'..'z') + ('0'..'9') + '-' + '_'
    }
}

/** What step 2 tells a newcomer about the group it is joining. */
internal class Welcome(
    val groupId: Bytes,
    val name: String,
    val expiresMinute: Long,
    val epoch: Long,
    val epochKey: ByteArray,
    val rosterSize: Int,
) {
    override fun toString() = "Welcome"
}

/**
 * The plaintexts of a join (ADR-0009). Each is fixed-size and zero-padded, and each parser reads
 * bytes from a phone that only proved it saw a QR code: it checks every length and never throws.
 */
internal object JoinPayloads {
    /** A handshake message is `e 32 ‖ payload ‖ tag 16`, filling the join frame. */
    const val HANDSHAKE_PAYLOAD_BYTES = Wire.JOIN_MESSAGE_BYTES - NoiseHandshake.KEY_BYTES - NoiseHandshake.TAG_BYTES

    /** A transport message is `plaintext ‖ tag 16`. */
    const val TRANSPORT_PAYLOAD_BYTES = Wire.JOIN_MESSAGE_BYTES - NoiseHandshake.TAG_BYTES

    private const val ENTRY_BYTES = Wire.PUBLIC_KEY_BYTES + 1 + GroupInner.NICKNAME_BYTES

    /** Roster entries in one transport message. */
    const val ENTRIES_PER_STEP = TRANSPORT_PAYLOAD_BYTES / ENTRY_BYTES

    /** The prologue both sides hash first. */
    val PROLOGUE = "echo join v1".encodeToByteArray()

    init {
        check(HANDSHAKE_PAYLOAD_BYTES == 180 && TRANSPORT_PAYLOAD_BYTES == 212 && ENTRIES_PER_STEP == 3) { "ADR-0009 sizes" }
    }

    /** Step 1: the newcomer's member key and nickname. */
    fun request(member: Member): ByteArray {
        val out = ByteBuffer.allocate(HANDSHAKE_PAYLOAD_BYTES)
        putEntry(out, member)
        return out.array()
    }

    fun parseRequest(payload: ByteArray): Member? {
        if (payload.size != HANDSHAKE_PAYLOAD_BYTES) return null
        val buffer = ByteBuffer.wrap(payload)
        val member = entry(buffer) ?: return null
        return member.takeIf { zeroRest(buffer) }
    }

    /** Step 2: the group, the current epoch key, and how many roster entries follow. */
    fun welcome(welcome: Welcome): ByteArray {
        val out = ByteBuffer.allocate(HANDSHAKE_PAYLOAD_BYTES)
        out.put(welcome.groupId.toByteArray())
        val name = GroupInner.nicknameBytes(welcome.name)
        out.put(name.size.toByte()).put(name.copyOf(GroupInner.NICKNAME_BYTES))
        out.putInt(welcome.expiresMinute.toInt()).putInt(welcome.epoch.toInt())
        out.put(welcome.epochKey).put(welcome.rosterSize.toByte())
        return out.array()
    }

    fun parseWelcome(payload: ByteArray): Welcome? {
        if (payload.size != HANDSHAKE_PAYLOAD_BYTES) return null
        val buffer = ByteBuffer.wrap(payload)
        val id = ByteArray(GroupInner.GROUP_ID_BYTES).also { buffer.get(it) }
        val name = GroupInner.nickname(buffer) ?: return null
        val expires = buffer.int.toLong() and U32_MAX
        val epoch = buffer.int.toLong() and U32_MAX
        val key = ByteArray(GroupEpochs.KEY_BYTES).also { buffer.get(it) }
        val rosterSize = buffer.get().toInt() and 0xFF
        if (rosterSize !in 1 until Group.MAX_MEMBERS || !zeroRest(buffer)) return null
        return Welcome(Bytes(id), name, expires, epoch, key, rosterSize)
    }

    /** Steps 3 on: up to [ENTRIES_PER_STEP] roster entries. */
    fun roster(members: List<Member>): ByteArray {
        require(members.size in 1..ENTRIES_PER_STEP) { "1 to $ENTRIES_PER_STEP entries a step" }
        val out = ByteBuffer.allocate(TRANSPORT_PAYLOAD_BYTES)
        members.forEach { putEntry(out, it) }
        return out.array()
    }

    /** Exactly [count] entries, the rest zero, or null. */
    fun parseRoster(
        payload: ByteArray,
        count: Int,
    ): List<Member>? {
        if (payload.size != TRANSPORT_PAYLOAD_BYTES || count !in 1..ENTRIES_PER_STEP) return null
        val buffer = ByteBuffer.wrap(payload)
        val members = List(count) { entry(buffer) ?: return null }
        return members.takeIf { zeroRest(buffer) }
    }

    private fun putEntry(
        out: ByteBuffer,
        member: Member,
    ) {
        val nickname = GroupInner.nicknameBytes(member.nickname)
        out.put(member.publicKey.toByteArray()).put(nickname.size.toByte()).put(nickname.copyOf(GroupInner.NICKNAME_BYTES))
    }

    private fun entry(buffer: ByteBuffer): Member? {
        val key = ByteArray(Wire.PUBLIC_KEY_BYTES).also { buffer.get(it) }
        val nickname = GroupInner.nickname(buffer) ?: return null
        return Member(Bytes(key), nickname)
    }

    private fun zeroRest(buffer: ByteBuffer): Boolean {
        while (buffer.hasRemaining()) if (buffer.get() != 0.toByte()) return false
        return true
    }

    private const val U32_MAX = 0xFFFF_FFFFL
}
