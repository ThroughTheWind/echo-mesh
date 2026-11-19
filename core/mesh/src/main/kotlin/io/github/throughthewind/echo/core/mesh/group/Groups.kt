package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.crypto.GroupCipher
import io.github.throughthewind.echo.core.crypto.GroupEpochs
import io.github.throughthewind.echo.core.crypto.MemberKey
import io.github.throughthewind.echo.core.crypto.Randomness
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.FrameKind
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Someone in a group, as this phone knows them: a member key and the nickname they chose (FR-8.4). */
data class Member(
    val publicKey: Bytes,
    val nickname: String,
) {
    val keyId: Bytes = GroupInner.keyId(publicKey)

    /** Never prints the key. */
    override fun toString() = "Member"
}

/**
 * A group this phone is in (FR-8): its keys, this phone's member key, and the roster. In memory;
 * the app seals it to disk through `core/data`, and [GroupBook.leave] or expiry destroys the keys.
 */
class Group internal constructor(
    /** Random, never on the air in clear: it is only inside signatures and the join (ADR-0009). */
    val id: Bytes,
    /** The local name, shown on this phone (FR-8.1). Joiners receive it inside the join. */
    val name: String,
    /** Minutes since the Unix epoch, UTC, when the group and its keys end (FR-8.6). */
    val expiresMinute: Long,
    internal val epochs: GroupEpochs,
    internal val me: MemberKey,
    val myNickname: String,
    roster: Collection<Member>,
) {
    private val members = LinkedHashMap<Bytes, Member>().apply { roster.forEach { put(it.keyId, it) } }

    /** This phone as a member. */
    val self: Member = Member(Bytes(me.publicKey), myNickname)

    init {
        members.getOrPut(self.keyId) { self }
    }

    /** Everyone this phone knows to be in the group, itself included. */
    val roster: List<Member> get() = members.values.toList()

    fun member(keyId: Bytes): Member? = members[keyId]

    internal fun add(member: Member): Boolean {
        if (members.size >= MAX_MEMBERS && member.keyId !in members) return false
        members.getOrPut(member.keyId) { member }
        return true
    }

    internal fun destroy() {
        epochs.destroy()
        me.destroy()
    }

    override fun toString() = "Group"

    companion object {
        /** The most members a roster holds (ADR-0009: 16 roster steps of 3 in a join). */
        const val MAX_MEMBERS = 48
    }
}

/** A group frame this phone could open: which group, who sent it, and what it says. */
class OpenedGroupFrame(
    val group: Group,
    val sender: Member,
    val message: GroupMessage,
)

/**
 * The groups this phone is in (FR-8.1, FR-8.5 to FR-8.7) and the group frame codec on top of them
 * (FR-9.1, ADR-0009). Not thread-safe: the mesh node serialises access.
 */
class GroupBook {
    private val groups = LinkedHashMap<Bytes, Group>()

    val all: List<Group> get() = groups.values.toList()

    operator fun get(id: Bytes): Group? = groups[id]

    /**
     * Creates a group named [name] that lives [lifetime] from [nowMinute] (FR-8.1), with this phone
     * as its first member under [nickname]. Null when this phone is already in [MAX_GROUPS] groups.
     */
    fun create(
        name: String,
        lifetime: Duration,
        nickname: String,
        nowMinute: Long,
    ): Group? {
        require(lifetime.isPositive() && lifetime <= MAX_LIFETIME) { "a group lives up to 7 days" }
        GroupInner.nicknameBytes(nickname)
        GroupInner.nicknameBytes(name)
        if (groups.size >= MAX_GROUPS) return null
        val group =
            Group(
                id = Bytes(Randomness.bytes(GroupInner.GROUP_ID_BYTES)),
                name = name,
                expiresMinute = nowMinute + lifetime.inWholeMinutes,
                epochs = GroupEpochs.create(epochOf(nowMinute)),
                me = MemberKey.generate(),
                myNickname = nickname,
                roster = emptyList(),
            )
        groups[group.id] = group
        return group
    }

    /** Adds a group received in a join. False when full, or already a member. */
    internal fun adopt(group: Group): Boolean {
        if (groups.size >= MAX_GROUPS || group.id in groups) return false
        groups[group.id] = group
        return true
    }

    /** Leaves [id]: its keys are destroyed on this phone (FR-8.5). */
    fun leave(id: Bytes) {
        groups.remove(id)?.destroy()
    }

    /**
     * Moves every group's keys to the epoch of [nowMinute], deleting old ones, and destroys groups
     * past their end (FR-8.6). Returns the groups that expired.
     */
    fun sweep(nowMinute: Long): List<Group> {
        val expired = groups.values.filter { nowMinute >= it.expiresMinute }
        expired.forEach { leave(it.id) }
        groups.values.forEach { it.epochs.advanceTo(epochOf(nowMinute)) }
        return expired
    }

    /** [message] from this phone to [group], as a group frame with [hopBudget] (FR-9.1). */
    fun seal(
        group: Group,
        message: GroupMessage,
        nowMinute: Long,
        hopBudget: Int,
    ): ByteArray {
        val epoch = epochOf(nowMinute)
        group.epochs.advanceTo(epoch)
        val key = checkNotNull(group.epochs.keyFor(epoch)) { "the current epoch key is always held" }
        val inner = GroupInner.encode(group.id, epoch, group.me, message)
        val sealed = GroupCipher.seal(key, inner, AAD)
        key.fill(0)
        return WireCodec.encode(
            GroupFrame(
                hopBudget = hopBudget,
                nonce = Bytes(sealed.copyOf(Wire.NONCE_BYTES)),
                ciphertext = Bytes(sealed.copyOfRange(Wire.NONCE_BYTES, sealed.size)),
            ),
        )
    }

    /**
     * Opens [frame] with each group's keys (no group id on the air: trial decryption, threat model
     * §5.3) and checks the sender's signature. Null for a frame of a group this phone is not in, a
     * frame from someone not on the roster, or one that does not verify. A member-added message
     * from a member adds the newcomer to the roster.
     */
    fun open(frame: GroupFrame): OpenedGroupFrame? {
        val sealed = frame.nonce.toByteArray() + frame.ciphertext.toByteArray()
        for (group in groups.values) {
            for (epoch in group.epochs.oldest..group.epochs.newest + 1) {
                val key = group.epochs.keyFor(epoch) ?: continue
                val inner = GroupCipher.open(key, sealed, AAD)
                key.fill(0)
                if (inner == null) continue
                val decoded = GroupInner.decode(inner) ?: return null
                val sender = group.member(decoded.senderKeyId) ?: return null
                if (!GroupInner.verify(group.id, epoch, decoded, sender.publicKey)) return null
                val message = decoded.message
                if (message is GroupMessage.MemberAdded) group.add(Member(message.member, message.nickname))
                return OpenedGroupFrame(group, sender, message)
            }
        }
        return null
    }

    companion object {
        /** FR-8.7: a phone is in at most 5 groups, which also bounds trial decryption. */
        const val MAX_GROUPS = 5
        val DEFAULT_LIFETIME: Duration = 24.hours
        val MAX_LIFETIME: Duration = 7.days

        /** The AEAD's associated data: the frame's version and kind (ADR-0009). */
        private val AAD = byteArrayOf(Wire.VERSION.toByte(), FrameKind.GROUP.code.toByte())

        /** The epoch of a minute: hours since the Unix epoch, UTC (threat model §5.3). */
        fun epochOf(minute: Long): Long = minute / 60
    }
}
