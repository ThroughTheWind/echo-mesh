package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.crypto.Hashes
import io.github.throughthewind.echo.core.crypto.MemberKey
import io.github.throughthewind.echo.core.crypto.Signatures
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.model.ReportType
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** What a member says to a group, inside a group frame (ADR-0009). */
sealed interface GroupMessage {
    val createdMinute: Long
    val lifetimeMinutes: Int

    /** A report only the group can read, shown in the verified style (FR-9.1, FR-9.2). */
    data class Report(
        val type: ReportType,
        /** 1/[Wire.DEGREE_UNITS] degree, as in public reports. */
        val latitude: Int,
        val longitude: Int,
        override val createdMinute: Long,
        override val lifetimeMinutes: Int,
    ) : GroupMessage

    /**
     * Confirms or dismisses a report, group or public, by its message id (FR-6.1). Counts once per
     * member key; the latest from a member is the one that counts (threat model §5.7).
     */
    data class ReportVote(
        val target: Bytes,
        val vote: Vote,
        override val createdMinute: Long,
        override val lifetimeMinutes: Int,
    ) : GroupMessage

    /** Withdraws one of the sender's own group reports, by its message id (FR-4.5). */
    data class Withdrawal(
        val target: Bytes,
        override val createdMinute: Long,
        override val lifetimeMinutes: Int,
    ) : GroupMessage

    /** A member let someone in: every join is visible to the group (FR-8.2, threat model §5.5). */
    data class MemberAdded(
        val member: Bytes,
        val nickname: String,
        override val createdMinute: Long,
        override val lifetimeMinutes: Int,
    ) : GroupMessage
}

/** An inner message that decoded; [signedBytes] still has to verify against the sender's key. */
class DecodedInner internal constructor(
    val senderKeyId: Bytes,
    val message: GroupMessage,
    internal val body: ByteArray,
    internal val signature: ByteArray,
)

/**
 * The 197-byte inner message of a group frame (ADR-0009): `sender key id 8 ‖ type 1 ‖ body 124 ‖
 * signature 64`, the signature by the sender's member key over `group id ‖ epoch ‖` the first 133
 * bytes. Unused bytes must be zero, so each message has one encoding. Decoding reads hostile bytes
 * the AEAD let through, from a member or a leaked key: it never throws.
 */
object GroupInner {
    const val KEY_ID_BYTES = 8
    const val BODY_BYTES = 124
    const val SIGNED_BYTES = KEY_ID_BYTES + 1 + BODY_BYTES
    const val SIZE = SIGNED_BYTES + Wire.SIGNATURE_BYTES
    const val GROUP_ID_BYTES = 16
    const val NICKNAME_BYTES = 32

    private const val REPORT = 0x01
    private const val VOTE = 0x02
    private const val WITHDRAWAL = 0x03
    private const val MEMBER_ADDED = 0x06

    init {
        check(SIZE == Wire.GROUP_PLAINTEXT_BYTES) { "the inner message must fill the group frame exactly" }
    }

    /** The first 8 bytes of SHA-256 of a member public key: how a frame names its sender. */
    fun keyId(publicKey: Bytes): Bytes = Bytes(Hashes.sha256(publicKey.toByteArray()).copyOf(KEY_ID_BYTES))

    /** Encodes and signs [message] from [sender] for [groupId] in [epoch]. */
    fun encode(
        groupId: Bytes,
        epoch: Long,
        sender: MemberKey,
        message: GroupMessage,
    ): ByteArray {
        val out = ByteArray(SIZE)
        keyId(Bytes(sender.publicKey)).toByteArray().copyInto(out, 0)
        val body = ByteBuffer.wrap(out, KEY_ID_BYTES + 1, BODY_BYTES)
        out[KEY_ID_BYTES] =
            when (message) {
                is GroupMessage.Report -> {
                    require(validPosition(message.latitude, message.longitude)) { "position out of range" }
                    body.put(message.type.code.toByte()).putInt(message.latitude).putInt(message.longitude)
                    REPORT
                }

                is GroupMessage.ReportVote -> {
                    require(message.target.size == Wire.MESSAGE_ID_BYTES) { "a message id is 32 bytes" }
                    body.put(message.target.toByteArray()).put(message.vote.code.toByte())
                    VOTE
                }

                is GroupMessage.Withdrawal -> {
                    require(message.target.size == Wire.MESSAGE_ID_BYTES) { "a message id is 32 bytes" }
                    body.put(message.target.toByteArray())
                    WITHDRAWAL
                }

                is GroupMessage.MemberAdded -> {
                    require(message.member.size == Wire.PUBLIC_KEY_BYTES) { "a member key is 32 bytes" }
                    val nickname = nicknameBytes(message.nickname)
                    body.put(message.member.toByteArray()).put(nickname.size.toByte()).put(nickname.copyOf(NICKNAME_BYTES))
                    MEMBER_ADDED
                }
            }.toByte()
        require(message.createdMinute in 0..U32_MAX && message.lifetimeMinutes in 1..Wire.MAX_LIFETIME_MINUTES) { "time out of range" }
        body.putInt(message.createdMinute.toInt()).putShort(message.lifetimeMinutes.toShort())
        sender.sign(signingInput(groupId, epoch, out.copyOf(SIGNED_BYTES))).copyInto(out, SIGNED_BYTES)
        return out
    }

    /** The layout of [inner], or null when it is not exactly one valid message. Not yet verified. */
    fun decode(inner: ByteArray): DecodedInner? {
        if (inner.size != SIZE) return null
        val body = ByteBuffer.wrap(inner, KEY_ID_BYTES + 1, BODY_BYTES)
        val message: GroupMessage =
            when (inner[KEY_ID_BYTES].toInt() and 0xFF) {
                REPORT -> {
                    val type = ReportType.of(body.get().toInt() and 0xFF) ?: return null
                    val latitude = body.int
                    val longitude = body.int
                    if (!validPosition(latitude, longitude)) return null
                    val (created, lifetime) = times(body) ?: return null
                    GroupMessage.Report(type, latitude, longitude, created, lifetime)
                }

                VOTE -> {
                    val target = ByteArray(Wire.MESSAGE_ID_BYTES).also { body.get(it) }
                    val vote = Vote.of(body.get().toInt() and 0xFF) ?: return null
                    val (created, lifetime) = times(body) ?: return null
                    GroupMessage.ReportVote(Bytes(target), vote, created, lifetime)
                }

                WITHDRAWAL -> {
                    val target = ByteArray(Wire.MESSAGE_ID_BYTES).also { body.get(it) }
                    val (created, lifetime) = times(body) ?: return null
                    GroupMessage.Withdrawal(Bytes(target), created, lifetime)
                }

                MEMBER_ADDED -> {
                    val member = ByteArray(Wire.PUBLIC_KEY_BYTES).also { body.get(it) }
                    val nickname = nickname(body) ?: return null
                    val (created, lifetime) = times(body) ?: return null
                    GroupMessage.MemberAdded(Bytes(member), nickname, created, lifetime)
                }

                else -> {
                    return null
                }
            }
        // Everything after the fields must be zero: one message, one encoding.
        while (body.hasRemaining()) if (body.get() != 0.toByte()) return null
        return DecodedInner(
            senderKeyId = Bytes(inner.copyOf(KEY_ID_BYTES)),
            message = message,
            body = inner.copyOf(SIGNED_BYTES),
            signature = inner.copyOfRange(SIGNED_BYTES, SIZE),
        )
    }

    /** True when [decoded] was signed by [publicKey] for [groupId] in [epoch]. */
    fun verify(
        groupId: Bytes,
        epoch: Long,
        decoded: DecodedInner,
        publicKey: Bytes,
    ): Boolean =
        keyId(publicKey) == decoded.senderKeyId &&
            Signatures.verify(publicKey.toByteArray(), decoded.signature, signingInput(groupId, epoch, decoded.body))

    /** UTF-8, at most [NICKNAME_BYTES]; refused before it is sent rather than cut (ADR-0009). */
    fun nicknameBytes(nickname: String): ByteArray {
        val bytes = nickname.encodeToByteArray()
        require(bytes.isNotEmpty() && bytes.size <= NICKNAME_BYTES) { "a nickname is 1 to $NICKNAME_BYTES UTF-8 bytes" }
        return bytes
    }

    /** A length-prefixed, zero-padded nickname, or null when the length, padding or UTF-8 is wrong. */
    internal fun nickname(buffer: ByteBuffer): String? {
        val length = buffer.get().toInt() and 0xFF
        val field = ByteArray(NICKNAME_BYTES).also { buffer.get(it) }
        if (length !in 1..NICKNAME_BYTES) return null
        if ((length until NICKNAME_BYTES).any { field[it] != 0.toByte() }) return null
        return try {
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(field, 0, length))
                .toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun times(body: ByteBuffer): Pair<Long, Int>? {
        val created = body.int.toLong() and U32_MAX
        val lifetime = body.short.toInt() and 0xFFFF
        return if (lifetime in 1..Wire.MAX_LIFETIME_MINUTES) created to lifetime else null
    }

    private fun signingInput(
        groupId: Bytes,
        epoch: Long,
        signed: ByteArray,
    ): ByteArray {
        require(groupId.size == GROUP_ID_BYTES) { "a group id is $GROUP_ID_BYTES bytes" }
        return groupId.toByteArray() + ByteBuffer.allocate(4).putInt(epoch.toInt()).array() + signed
    }

    private fun validPosition(
        latitude: Int,
        longitude: Int,
    ) = latitude in -90 * Wire.DEGREE_UNITS..90 * Wire.DEGREE_UNITS &&
        longitude in -180 * Wire.DEGREE_UNITS..180 * Wire.DEGREE_UNITS

    private const val U32_MAX = 0xFFFF_FFFFL
}
