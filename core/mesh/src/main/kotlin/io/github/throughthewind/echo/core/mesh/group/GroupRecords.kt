package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.crypto.GroupEpochs
import io.github.throughthewind.echo.core.crypto.MemberKey
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Wire
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Where groups are kept between runs: small sealed files, which the app backs with `core/data`'s
 * `SealedFiles` under its Keystore key (FR-12.1, threat model §5.6). The mesh never sees the key.
 */
interface SealedRecords {
    fun names(): Set<String>

    /** The record, or null when it is missing or cannot be opened (key gone, phone locked, tampered). */
    fun read(name: String): ByteArray?

    fun write(
        name: String,
        plaintext: ByteArray,
    )

    fun delete(name: String)
}

/**
 * One group as it is kept on this phone: its facts, the epoch keys held now, this phone's member
 * key and the roster. Local only, never on the air, so it is versioned but not padded. Decoding a
 * record that does not parse gives null; it never throws.
 */
internal object GroupRecord {
    const val PREFIX = "group-"

    /** 2 adds the muted members (WI-021); a version 1 record reads as muting nobody. */
    private const val VERSION = 2
    private const val VERSION_WITHOUT_MUTES = 1
    private const val MAX_EPOCHS = 16

    fun name(id: Bytes): String = PREFIX + id.toByteArray().joinToString("") { "%02x".format(it) }

    fun encode(group: Group): ByteArray {
        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        out.writeByte(VERSION)
        out.write(group.id.toByteArray())
        text(out, group.name)
        out.writeLong(group.expiresMinute)
        text(out, group.myNickname)
        val seed = group.me.exportSeed()
        out.write(seed)
        seed.fill(0)
        val epochs = group.epochs.export()
        out.writeByte(epochs.size)
        epochs.forEach { (epoch, key) ->
            out.writeLong(epoch)
            out.write(key)
            key.fill(0)
        }
        val roster = group.roster
        out.writeByte(roster.size)
        roster.forEach {
            out.write(it.publicKey.toByteArray())
            text(out, it.nickname)
        }
        val muted = group.muted
        out.writeByte(muted.size)
        muted.forEach { out.write(it.toByteArray()) }
        out.flush()
        return bytes.toByteArray()
    }

    fun decode(record: ByteArray): Group? {
        val input = DataInputStream(ByteArrayInputStream(record))
        val keys = ArrayList<Pair<Long, ByteArray>>()
        val seed = ByteArray(MemberKey.SEED_BYTES)
        return try {
            val version = input.readUnsignedByte()
            if (version != VERSION && version != VERSION_WITHOUT_MUTES) return null
            val id = ByteArray(GroupInner.GROUP_ID_BYTES).also { input.readFully(it) }
            val name = text(input) ?: return null
            val expires = input.readLong()
            val nickname = text(input) ?: return null
            input.readFully(seed)
            val epochCount = input.readUnsignedByte()
            if (epochCount !in 1..MAX_EPOCHS) return null
            repeat(epochCount) {
                val epoch = input.readLong()
                keys += epoch to ByteArray(GroupEpochs.KEY_BYTES).also { input.readFully(it) }
            }
            if (keys.zipWithNext().any { (a, b) -> b.first != a.first + 1 }) return null
            val rosterCount = input.readUnsignedByte()
            if (rosterCount !in 1..Group.MAX_MEMBERS) return null
            val roster =
                List(rosterCount) {
                    val key = ByteArray(Wire.PUBLIC_KEY_BYTES).also { input.readFully(it) }
                    Member(Bytes(key), text(input) ?: return null)
                }
            val muted =
                if (version == VERSION_WITHOUT_MUTES) {
                    emptyList()
                } else {
                    val count = input.readUnsignedByte()
                    if (count >= rosterCount) return null
                    List(count) { Bytes(ByteArray(GroupInner.KEY_ID_BYTES).also { input.readFully(it) }) }
                }
            val ids = roster.map { it.keyId }.toSet()
            if (muted.any { it !in ids } || muted.toSet().size != muted.size) return null
            if (input.available() != 0) return null
            Group(
                id = Bytes(id),
                name = name,
                expiresMinute = expires,
                epochs = GroupEpochs.restore(keys),
                me = MemberKey.fromSeed(seed),
                myNickname = nickname,
                roster = roster,
                muted = muted,
            )
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            seed.fill(0)
            keys.forEach { it.second.fill(0) }
        }
    }

    private fun text(
        out: DataOutputStream,
        value: String,
    ) {
        val bytes = GroupInner.nicknameBytes(value)
        out.writeByte(bytes.size)
        out.write(bytes)
    }

    private fun text(input: DataInputStream): String? {
        val length = input.readUnsignedByte()
        if (length !in 1..GroupInner.NICKNAME_BYTES) return null
        val bytes = ByteArray(length).also { input.readFully(it) }
        return try {
            bytes.decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }
    }
}
