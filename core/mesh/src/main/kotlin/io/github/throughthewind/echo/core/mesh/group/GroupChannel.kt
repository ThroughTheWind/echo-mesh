package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.crypto.GroupEpochs
import io.github.throughthewind.echo.core.crypto.MemberKey
import io.github.throughthewind.echo.core.crypto.NoiseHandshake
import io.github.throughthewind.echo.core.crypto.NoiseSession
import io.github.throughthewind.echo.core.crypto.NoiseStaticKey
import io.github.throughthewind.echo.core.crypto.Randomness
import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.relay.HeldFrame
import io.github.throughthewind.echo.core.mesh.relay.Relay
import io.github.throughthewind.echo.core.mesh.report.CastVote
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.VoteRule
import io.github.throughthewind.echo.core.mesh.report.WireUnits
import io.github.throughthewind.echo.core.mesh.report.instantOfWireMinute
import io.github.throughthewind.echo.core.mesh.report.wireMinute
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.JoinFrame
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.Report
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A group as the screens show it: never its keys. */
data class GroupInfo(
    val id: Bytes,
    val name: String,
    val expiresAt: Instant,
    val myNickname: String,
    /** Nicknames of the members this phone knows of, itself included (FR-8.7). */
    val members: List<String>,
    /** The same members with what the roster needs to mute one (FR-9.4). */
    val roster: List<MemberInfo> = emptyList(),
)

/** A member as the roster shows them: never their key, only its id to mute them by. */
data class MemberInfo(
    val id: Bytes,
    val nickname: String,
    val isMe: Boolean,
    /** Hidden on this phone only (FR-9.4). */
    val muted: Boolean,
    /** When this phone last heard from them, if within [GroupChannel.SEEN_FOR]; null otherwise (FR-8.7). */
    val lastSeen: Instant? = null,
)

/** A group report this phone could open (FR-9.1): who in which group said it. */
data class GroupReport(
    val id: Bytes,
    val groupId: Bytes,
    val groupName: String,
    val sender: String,
    val mine: Boolean,
    val report: Report,
    /** Hops it may still travel from this phone. */
    val hopsLeft: Int,
)

/** Where a member of one of this phone's groups last said they were (FR-10.3). Memory only. */
data class MemberPosition(
    val groupId: Bytes,
    val groupName: String,
    /** The member key id: one member, even if two share a nickname. */
    val member: Bytes,
    val nickname: String,
    val position: GeoPoint,
    /** When the member's phone took it, never later than when it arrived. */
    val at: Instant,
    /** After this it is not shown at all (FR-10.2). */
    val expiresAt: Instant,
)

/** Someone joined one of this phone's groups (FR-8.2): every join is visible to the group. */
data class JoinEvent(
    val groupId: Bytes,
    val groupName: String,
    val nickname: String,
    /** True on the phone whose code was used: it shows the "not the person in front of you?" guidance. */
    val throughThisPhone: Boolean,
)

/**
 * This phone's groups on the mesh (WI-018): it seals the user's group messages and hands them to the
 * relay like any frame, and opens every group frame the relay stores, from the air or its own.
 * Everyone relays group frames (FR-5.4); only this layer reads them, and only for groups it is in.
 * It also runs both sides of a join in person (FR-8.2, ADR-0009): [invite] and [join].
 */
class GroupChannel(
    private val relay: Relay,
    private val clock: () -> Instant,
    private val hopBudget: Int,
    /** Where groups are kept between runs; null keeps them in memory only (tests, no store). */
    private val vault: SealedRecords? = null,
) {
    private val lock = Mutex()
    private val book = GroupBook()
    private val held = LinkedHashMap<Bytes, GroupReport>()

    /** Who sent each held report, by member key id: only they may withdraw it. */
    private val authors = HashMap<Bytes, Bytes>()

    /** Group votes by target report, then by member key id: a member's latest counts (§5.7). */
    private val groupVotes = LinkedHashMap<Bytes, LinkedHashMap<Bytes, CastVote>>()
    private val voteList = MutableStateFlow<Map<Bytes, List<CastVote>>>(emptyMap())

    /** Votes from members of this phone's groups, by the report they are on (FR-6.2). */
    val votes: StateFlow<Map<Bytes, List<CastVote>>> = voteList.asStateFlow()

    /** The latest position per member, by group then member key id; in memory, never stored (FR-10.3). */
    private val latest = LinkedHashMap<Pair<Bytes, Bytes>, MemberPosition>()
    private val positionList = MutableStateFlow<List<MemberPosition>>(emptyList())

    /** Members' latest shared positions, not yet expired, from every group this phone is in. */
    val positions: StateFlow<List<MemberPosition>> = positionList.asStateFlow()

    /** When each member was last heard from, by group then key id; memory only, nothing older than [SEEN_FOR] (FR-8.7). */
    private val seen = HashMap<Pair<Bytes, Bytes>, Instant>()

    /** Withdrawals seen, target id to the key id that signed them, even before the report arrives. */
    private val withdrawals = HashMap<Bytes, Bytes>()

    /** Open QR codes, at most one per group: each lets in one newcomer, then is destroyed (AC-25). */
    private val invitations = LinkedHashMap<Bytes, Invitation>()

    /** The newest epoch each group's record holds, so a sweep rewrites a record only when its keys moved. */
    private val savedEpochs = HashMap<Bytes, Long>()

    private val groupList = MutableStateFlow<List<GroupInfo>>(emptyList())
    private val shown = MutableStateFlow<List<GroupReport>>(emptyList())

    val groups: StateFlow<List<GroupInfo>> = groupList.asStateFlow()

    private val joinEvents = MutableSharedFlow<JoinEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Joins as this phone learns of them, through its own code or a member-added frame. */
    val joins: SharedFlow<JoinEvent> = joinEvents.asSharedFlow()

    /** Live, not withdrawn group reports from every group this phone is in. */
    val reports: StateFlow<List<GroupReport>> = shown.asStateFlow()

    /**
     * Loads the groups kept on this phone, then opens group frames as the relay stores them and
     * answers newcomers who scanned a code.
     */
    fun run(scope: CoroutineScope): Job =
        scope.launch {
            launch(start = CoroutineStart.UNDISPATCHED) { relay.groupFrames.collect { if (open(it)) rescan() } }
            launch(start = CoroutineStart.UNDISPATCHED) { relay.joinFrames.collect { (peer, frame) -> answer(peer, frame) } }
            restore()
            rescan()
        }

    /**
     * Loads the groups kept on this phone (FR-12.1). A record past its group's end is deleted, not
     * kept (FR-12.4); one that cannot be opened, as while the phone is locked, is left alone.
     */
    suspend fun restore() {
        val vault = vault ?: return
        lock.withLock {
            val minute = clock().wireMinute()
            for (name in vault.names().filter { it.startsWith(GroupRecord.PREFIX) }.sorted()) {
                val record = vault.read(name) ?: continue
                val group = GroupRecord.decode(record)
                record.fill(0)
                if (group == null || GroupRecord.name(group.id) != name) continue
                if (minute >= group.expiresMinute) {
                    group.destroy()
                    vault.delete(name)
                    continue
                }
                if (book[group.id] != null || !book.adopt(group)) {
                    group.destroy()
                    continue
                }
                savedEpochs[group.id] = group.epochs.newest
            }
            saveMoved(minute)
            publishGroups()
        }
    }

    /** Creates a group (FR-8.1). Null when this phone is already in five. */
    suspend fun create(
        name: String,
        nickname: String,
        lifetime: Duration = GroupBook.DEFAULT_LIFETIME,
    ): GroupInfo? =
        lock.withLock {
            book
                .create(name, lifetime, nickname, clock().wireMinute())
                ?.also {
                    save(it)
                    publishGroups()
                }?.info()
        }

    /** Leaves [id] (FR-8.5): its keys go, and so do its reports on this phone. */
    suspend fun leave(id: Bytes) {
        lock.withLock {
            book.leave(id)
            forget(id)
            invitations.remove(id)?.destroy()
            held.values.removeAll { it.groupId == id }
            authors.keys.retainAll(held.keys)
            latest.keys.removeAll { it.first == id }
            seen.keys.removeAll { it.first == id }
            publish()
        }
    }

    /**
     * Sends where this phone is, [position] taken [at], to the group [id] only (FR-10.2). It expires
     * after [POSITION_LIFETIME] on every phone. False when this phone is not in the group.
     */
    suspend fun sharePosition(
        id: Bytes,
        position: GeoPoint,
        at: Instant,
    ): Boolean {
        val minute = at.wireMinute()
        val frame =
            lock.withLock {
                val group = book[id] ?: return false
                val message =
                    GroupMessage.Position(
                        WireUnits.of(position.latitude),
                        WireUnits.of(position.longitude),
                        (at.epochSeconds - minute * 60).toInt(),
                        minute,
                        POSITION_LIFETIME.inWholeMinutes.toInt(),
                    )
                book.seal(group, message, clock().wireMinute(), hopBudget)
            }
        return relay.publish(frame) is Validation.Opaque
    }

    /** Sends a report of [type] at [position] to the group [id] (FR-9.1). Its message id, or null. */
    suspend fun sendReport(
        id: Bytes,
        type: ReportType,
        position: GeoPoint,
        lifetime: Duration = type.defaultLifetime,
    ): Bytes? {
        val minute = clock().wireMinute()
        val frame =
            lock.withLock {
                val group = book[id] ?: return null
                val message =
                    GroupMessage.Report(
                        type,
                        WireUnits.of(position.latitude),
                        WireUnits.of(position.longitude),
                        minute,
                        lifetime.inWholeMinutes.toInt(),
                    )
                book.seal(group, message, minute, hopBudget)
            }
        return (relay.publish(frame) as? Validation.Opaque)?.id
    }

    /**
     * Sends this phone's [vote] on the report [target] to the group [id] (FR-6.1), living until
     * [untilMinute], the latest the report could live. False when this phone is not in the group.
     */
    suspend fun vote(
        id: Bytes,
        target: Bytes,
        vote: Vote,
        untilMinute: Long,
    ): Boolean {
        val minute = clock().wireMinute()
        val frame =
            lock.withLock {
                val group = book[id] ?: return false
                val left = (untilMinute - minute).toInt().coerceIn(1, Wire.MAX_LIFETIME_MINUTES)
                book.seal(group, GroupMessage.ReportVote(target, vote, minute, left), minute, hopBudget)
            }
        return relay.publish(frame) is Validation.Opaque
    }

    /** Withdraws this phone's own group report [id] (FR-4.5). False if it is not ours or is gone. */
    suspend fun withdraw(id: Bytes): Boolean {
        val minute = clock().wireMinute()
        val frame =
            lock.withLock {
                val report = held[id]?.takeIf { it.mine } ?: return false
                val group = book[report.groupId] ?: return false
                val left = ((report.report.expiresAt.epochSeconds / 60) - minute).toInt().coerceIn(1, Wire.MAX_LIFETIME_MINUTES)
                book.seal(group, GroupMessage.Withdrawal(id, minute, left), minute, hopBudget)
            }
        return relay.publish(frame) is Validation.Opaque
    }

    /**
     * Mutes or unmutes the member [member] of the group [id] on this phone only (FR-9.4): their
     * reports, votes and position are hidden at once (AC-24), and still relayed, so the rest of the
     * mesh is not affected. Kept with the group. False for yourself or someone not on the roster.
     */
    suspend fun mute(
        id: Bytes,
        member: Bytes,
        muted: Boolean,
    ): Boolean =
        lock.withLock {
            val group = book[id] ?: return false
            if (!group.mute(member, muted)) return false
            save(group)
            publish()
            true
        }

    /**
     * The panic wipe's part here (FR-11.2): destroys every group's keys in memory and forgets
     * everything read from the groups. Writes nothing: the sealed store is already gone.
     */
    suspend fun wipe() {
        lock.withLock {
            invitations.values.forEach { it.destroy() }
            invitations.clear()
            book.all.map { it.id }.forEach { book.leave(it) }
            held.clear()
            authors.clear()
            groupVotes.clear()
            withdrawals.clear()
            latest.clear()
            seen.clear()
            savedEpochs.clear()
            publish()
        }
    }

    /** Ratchets keys, ends expired groups (FR-8.6) and drops expired reports. */
    suspend fun sweep() {
        lock.withLock {
            val now = clock()
            book.sweep(now.wireMinute()).forEach { forget(it.id) }
            saveMoved(now.wireMinute())
            dropInvitations(now)
            held.values.removeAll { it.report.isExpired(now) || book[it.groupId] == null }
            authors.keys.retainAll(held.keys)
            if (withdrawals.size > MAX_WITHDRAWALS) withdrawals.clear()
            // A vote outlives its report by at most the longest lifetime; then it is forgotten.
            val minute = now.wireMinute()
            groupVotes.values.removeAll { byMember -> byMember.values.all { it.minute + VOTE_MEMORY_MINUTES < minute } }
            latest.values.removeAll { it.expiresAt <= now || book[it.groupId] == null }
            seen.entries.removeAll { (key, at) -> now - at > SEEN_FOR || book[key.first] == null }
            publish()
        }
    }

    /**
     * Opens what the relay already holds, for a group just joined, or once a member is added and
     * their frames that arrived first can be attributed. Repeats while the roster grows.
     */
    suspend fun rescan() {
        do {
            val grew = relay.heldGroupFrames().map { open(it) }.any { it }
        } while (grew)
    }

    /**
     * A QR code that lets one newcomer into the group [id] (FR-8.2): it holds a one-time key and
     * token, works once within [lifetime], and replaces any earlier code for the group. Null when
     * this phone is not in the group or the group is full.
     */
    suspend fun invite(
        id: Bytes,
        lifetime: Duration = INVITATION_LIFETIME,
    ): JoinOffer? =
        lock.withLock {
            val group = book[id] ?: return null
            if (group.roster.size >= Group.MAX_MEMBERS) return null
            val key = NoiseStaticKey.generate()
            val offer = JoinOffer(Bytes(key.publicKey), Bytes(Randomness.bytes(JoinOffer.TOKEN_BYTES)))
            invitations.remove(id)?.destroy()
            invitations[id] = Invitation(id, key, offer.psk(), clock() + lifetime)
            offer
        }

    /**
     * Renames the group [id] on this phone only: nobody else sees it, and nothing is sent. False
     * when this phone is not in the group. A name is 1 to 32 UTF-8 bytes, like a nickname.
     */
    suspend fun rename(
        id: Bytes,
        name: String,
    ): Boolean {
        GroupInner.nicknameBytes(name)
        return lock.withLock {
            val group = book[id] ?: return false
            group.name = name
            held.replaceAll { _, report -> if (report.groupId == id) report.copy(groupName = name) else report }
            save(group)
            publish()
            true
        }
    }

    /** Takes back the QR code for [id], if one is open. */
    suspend fun cancelInvite(id: Bytes) {
        lock.withLock { invitations.remove(id)?.destroy() }
    }

    /**
     * Joins the group behind [offer], scanned from a member's phone in direct range, as [nickname]
     * (FR-8.2, ADR-0009). The newcomer gets the current epoch key only, never older traffic. Null
     * when the code is used, expired or not for a phone in range, when the exchange is tampered
     * with, or when this phone is already in five groups: the newcomer scans a fresh code.
     */
    suspend fun join(
        offer: JoinOffer,
        nickname: String,
        timeout: Duration = JOIN_TIMEOUT,
    ): GroupInfo? {
        GroupInner.nicknameBytes(nickname)
        if (lock.withLock { book.all.size >= GroupBook.MAX_GROUPS }) return null
        val me = MemberKey.generate()
        val self = Member(Bytes(me.publicKey), nickname)
        val session = Bytes(Randomness.bytes(Wire.JOIN_SESSION_BYTES))
        val handshake = NoiseHandshake.initiator(offer.responderKey.toByteArray(), offer.psk(), JoinPayloads.PROLOGUE)
        val first = JoinFrame(session, 1, Bytes(handshake.writeMessage(JoinPayloads.request(self))))
        val group =
            withTimeoutOrNull(timeout) {
                coroutineScope {
                    val inbox = Channel<Pair<PeerHandle, JoinFrame>>(INBOX_CAPACITY)
                    val listener =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            relay.joinFrames.collect { if (it.second.session == session) inbox.trySend(it) }
                        }
                    try {
                        relay.sendJoin(null, first)
                        receiveJoin(inbox, handshake, me, nickname)
                    } finally {
                        listener.cancel()
                    }
                }
            }
        if (group == null) {
            me.destroy()
            return null
        }
        val info =
            lock.withLock {
                if (!book.adopt(group)) return@withLock null
                save(group)
                publishGroups()
                group.info()
            }
        if (info == null) {
            group.destroy()
            return null
        }
        rescan()
        return info
    }

    /** The newcomer's side after step 1: the welcome, then the roster from the same phone, in order. */
    private suspend fun receiveJoin(
        inbox: Channel<Pair<PeerHandle, JoinFrame>>,
        handshake: NoiseHandshake,
        me: MemberKey,
        nickname: String,
    ): Group? {
        var welcome: Welcome? = null
        var inviter: PeerHandle? = null
        var transport: NoiseSession? = null
        val roster = ArrayList<Member>()
        for ((peer, frame) in inbox) {
            val known = welcome
            if (known == null) {
                if (frame.step != 2) continue
                // A failed read kills the handshake: whoever answered was not the code's phone.
                val payload = handshake.readMessage(frame.message.toByteArray()) ?: return null
                welcome = JoinPayloads.parseWelcome(payload)?.takeIf { plausible(it) } ?: return null
                inviter = peer
                transport = handshake.split()
                continue
            }
            if (peer != inviter || frame.step != FIRST_ROSTER_STEP + roster.size / JoinPayloads.ENTRIES_PER_STEP) continue
            val plain = transport!!.decrypt(frame.message.toByteArray()) ?: return null
            val count = minOf(JoinPayloads.ENTRIES_PER_STEP, known.rosterSize - roster.size)
            roster += JoinPayloads.parseRoster(plain, count) ?: return null
            if (roster.size == known.rosterSize) {
                return Group(
                    id = known.groupId,
                    name = known.name,
                    expiresMinute = known.expiresMinute,
                    epochs = GroupEpochs.fromKey(known.epoch, known.epochKey).also { known.epochKey.fill(0) },
                    me = me,
                    myNickname = nickname,
                    roster = roster,
                )
            }
        }
        return null
    }

    /** A welcome whose clock and lifetime make sense on this phone; anything else is refused. */
    private fun plausible(welcome: Welcome): Boolean {
        val minute = clock().wireMinute()
        return abs(welcome.epoch - GroupBook.epochOf(minute)) <= GroupEpochs.MAX_AHEAD &&
            welcome.expiresMinute > minute &&
            welcome.expiresMinute <= minute + GroupBook.MAX_LIFETIME.inWholeMinutes
    }

    /**
     * The member's side: a step 1 that opens under one of this phone's codes gets the welcome and
     * the roster, the code is destroyed, and the group is told who joined (FR-8.2). Anything else
     * is dropped without an answer, so a stranger learns nothing.
     */
    private suspend fun answer(
        peer: PeerHandle,
        frame: JoinFrame,
    ) {
        if (frame.step != 1) return
        val now = clock()
        val minute = now.wireMinute()
        val (replies, announcement) =
            lock.withLock {
                dropInvitations(now)
                var opened: Pair<Invitation, Pair<NoiseHandshake, ByteArray>>? = null
                for (invitation in invitations.values) {
                    val handshake = NoiseHandshake.responder(invitation.key, invitation.psk, JoinPayloads.PROLOGUE)
                    val payload = handshake.readMessage(frame.message.toByteArray()) ?: continue
                    opened = invitation to (handshake to payload)
                    break
                }
                val (invitation, read) = opened ?: return
                val (handshake, payload) = read
                // Used once, whatever happens next (AC-25).
                invitations.remove(invitation.groupId)?.destroy()
                val joiner = JoinPayloads.parseRequest(payload) ?: return
                val group = book[invitation.groupId] ?: return
                val roster = group.roster
                if (roster.size >= Group.MAX_MEMBERS || group.member(joiner.keyId) != null) return
                val epoch = GroupBook.epochOf(minute)
                group.epochs.advanceTo(epoch)
                val key = checkNotNull(group.epochs.keyFor(epoch)) { "the current epoch key is always held" }
                val welcome = Welcome(group.id, group.name, group.expiresMinute, epoch, key, roster.size)
                val second = handshake.writeMessage(JoinPayloads.welcome(welcome))
                key.fill(0)
                val session = handshake.split()
                val steps =
                    listOf(JoinFrame(frame.session, 2, Bytes(second))) +
                        roster.chunked(JoinPayloads.ENTRIES_PER_STEP).mapIndexed { i, chunk ->
                            JoinFrame(frame.session, FIRST_ROSTER_STEP + i, Bytes(session.encrypt(JoinPayloads.roster(chunk))))
                        }
                group.add(joiner)
                save(group)
                // The newcomer is in front of this phone: seen now.
                markSeen(group.id, joiner.keyId, clock())
                joinEvents.tryEmit(JoinEvent(group.id, group.name, joiner.nickname, throughThisPhone = true))
                val added = GroupMessage.MemberAdded(joiner.publicKey, joiner.nickname, minute, Wire.MAX_LIFETIME_MINUTES)
                publishGroups()
                steps to book.seal(group, added, minute, hopBudget)
            }
        replies.forEach { relay.sendJoin(peer, it) }
        relay.publish(announcement)
    }

    private fun dropInvitations(now: Instant) {
        invitations.values.filter { now >= it.expiresAt }.forEach { invitations.remove(it.groupId)?.destroy() }
    }

    private class Invitation(
        val groupId: Bytes,
        val key: NoiseStaticKey,
        val psk: ByteArray,
        val expiresAt: Instant,
    ) {
        fun destroy() {
            key.destroy()
            psk.fill(0)
        }

        override fun toString() = "Invitation"
    }

    internal suspend fun <T> withBook(action: (GroupBook) -> T): T =
        lock.withLock {
            val result = action(book)
            book.all.forEach { save(it) }
            publishGroups()
            result
        }

    /** Opens [frame] if it is for one of this phone's groups. True when it grew a roster. */
    private suspend fun open(frame: HeldFrame): Boolean {
        val group = frame.frame as? GroupFrame ?: return false
        return lock.withLock {
            if (frame.id in held) return false
            val members = book.all.sumOf { it.roster.size }
            val opened = book.open(group) ?: return false
            val said = opened.message
            val sent = instantOfWireMinute(said.createdMinute) + ((said as? GroupMessage.Position)?.second ?: 0).seconds
            val heard = minOf(sent, clock())
            markSeen(opened.group.id, opened.sender.keyId, heard)
            // A newcomer was let in in person by the sender, at the same time.
            if (said is GroupMessage.MemberAdded) markSeen(opened.group.id, GroupInner.keyId(said.member), heard)
            when (val message = opened.message) {
                is GroupMessage.Report -> {
                    val report =
                        Report(
                            type = message.type,
                            position = GeoPoint(WireUnits.degrees(message.latitude), WireUnits.degrees(message.longitude)),
                            createdAt = instantOfWireMinute(message.createdMinute),
                            lifetime = message.lifetimeMinutes.minutes,
                        )
                    if (report.isExpired(clock()) || withdrawals[frame.id] == opened.sender.keyId) return false
                    authors[frame.id] = opened.sender.keyId
                    held[frame.id] =
                        GroupReport(
                            id = frame.id,
                            groupId = opened.group.id,
                            groupName = opened.group.name,
                            sender = opened.sender.nickname,
                            mine = opened.sender.keyId == opened.group.self.keyId,
                            report = report,
                            hopsLeft = frame.hopsLeft,
                        )
                }

                is GroupMessage.ReportVote -> {
                    val cast = CastVote(opened.sender.keyId, fromGroup = true, vote = message.vote, minute = message.createdMinute)
                    if (message.target !in groupVotes && groupVotes.size >= MAX_VOTE_TARGETS) {
                        groupVotes.remove(groupVotes.keys.first())
                    }
                    val byMember = groupVotes.getOrPut(message.target) { LinkedHashMap() }
                    val previous = byMember[cast.voter]
                    if (previous == null || previous.minute <= cast.minute) byMember[cast.voter] = cast
                    if (message.vote == Vote.CONFIRM && !opened.group.isMuted(opened.sender.keyId)) {
                        // A confirmation refreshes the report's time left, capped (FR-6.3); a muted member's does not.
                        val report = held[message.target]
                        if (report != null) {
                            val r = report.report
                            val until =
                                VoteRule.extendedBy(
                                    r.createdAt.wireMinute(),
                                    r.lifetime.inWholeMinutes.toInt(),
                                    r.type.defaultLifetime.inWholeMinutes
                                        .toInt(),
                                    message.createdMinute,
                                )
                            val lifetime = (until - r.createdAt.wireMinute()).minutes
                            if (lifetime > r.lifetime) held[message.target] = report.copy(report = r.copy(lifetime = lifetime))
                        } else {
                            relay.groupConfirmed(message.target, message.createdMinute)
                        }
                    }
                }

                is GroupMessage.Withdrawal -> {
                    // Only the member who sent a report can withdraw it.
                    withdrawals[message.target] = opened.sender.keyId
                    if (authors[message.target] == opened.sender.keyId) {
                        held.remove(message.target)
                        authors.remove(message.target)
                    }
                }

                is GroupMessage.Position -> {
                    val now = clock()
                    if (opened.sender.keyId == opened.group.self.keyId) return false
                    val taken = instantOfWireMinute(message.createdMinute) + message.second.seconds
                    val at = minOf(taken, now)
                    val expiresAt = taken + minOf(message.lifetimeMinutes.minutes, POSITION_LIFETIME)
                    if (expiresAt <= now) return false
                    val key = opened.group.id to opened.sender.keyId
                    val previous = latest[key]
                    if (previous == null || previous.at < at) {
                        latest[key] =
                            MemberPosition(
                                groupId = opened.group.id,
                                groupName = opened.group.name,
                                member = opened.sender.keyId,
                                nickname = opened.sender.nickname,
                                position = GeoPoint(WireUnits.degrees(message.latitude), WireUnits.degrees(message.longitude)),
                                at = at,
                                expiresAt = expiresAt,
                            )
                    }
                }

                is GroupMessage.MemberAdded -> {
                    if (book.all.sumOf { it.roster.size } > members) {
                        joinEvents.tryEmit(JoinEvent(opened.group.id, opened.group.name, message.nickname, throughThisPhone = false))
                    }
                }
            }
            publish()
            (book.all.sumOf { it.roster.size } > members).also { grew -> if (grew) save(opened.group) }
        }
    }

    private fun markSeen(
        group: Bytes,
        member: Bytes,
        at: Instant,
    ) {
        if (clock() - at > SEEN_FOR) return
        val key = group to member
        val previous = seen[key]
        if (previous == null || previous < at) seen[key] = at
    }

    /** Writes [group]'s record: keys, roster and all, sealed by the vault. */
    private fun save(group: Group) {
        val vault = vault ?: return
        val record = GroupRecord.encode(group)
        try {
            vault.write(GroupRecord.name(group.id), record)
            savedEpochs[group.id] = group.epochs.newest
        } catch (_: IOException) {
            // The group stays in memory; the next sweep tries again.
            savedEpochs.remove(group.id)
        } finally {
            record.fill(0)
        }
    }

    /** Rewrites the records whose keys ratcheted, so a deleted epoch key is gone from storage too. */
    private fun saveMoved(minute: Long) {
        book.all.forEach {
            it.epochs.advanceTo(GroupBook.epochOf(minute))
            if (savedEpochs[it.id] != it.epochs.newest) save(it)
        }
    }

    /** Deletes [id]'s record (FR-8.5, FR-8.6, FR-12.4). */
    private fun forget(id: Bytes) {
        savedEpochs.remove(id)
        vault?.delete(GroupRecord.name(id))
    }

    companion object {
        /** How long a QR code stays usable if nobody scans it (design: QrJoinFlow, "new code in 1:48"). */
        val INVITATION_LIFETIME: Duration = 2.minutes

        /** How long a newcomer waits for the whole join before scanning again. */
        val JOIN_TIMEOUT: Duration = 30.seconds

        /** Members not heard from for this long drop off the roster; nobody's history is kept (screens/GroupRoster). */
        val SEEN_FOR: Duration = 15.minutes

        /** How long a shared position is shown before it disappears ("a few minutes", FR-10.2; tuned in WI-028). */
        val POSITION_LIFETIME: Duration = 5.minutes

        /** Withdrawals remembered for reports not seen yet; bounded like everything a peer can fill. */
        private const val MAX_WITHDRAWALS = 512

        /** Reports with group votes remembered; bounded like everything a peer can fill. */
        private const val MAX_VOTE_TARGETS = 512
        private const val VOTE_MEMORY_MINUTES = 2L * Wire.MAX_LIFETIME_MINUTES
        private const val FIRST_ROSTER_STEP = 3
        private const val INBOX_CAPACITY = 32
    }

    private fun publish() {
        val now = clock()
        // A muted member's reports, votes and position are hidden here, on this phone only (FR-9.4).
        val muted = book.all.flatMap { it.muted }.toSet()
        shown.value = held.values.filter { !it.report.isExpired(now) && !book[it.groupId].isMutedHere(authors[it.id]) }
        positionList.value = latest.values.filter { it.expiresAt > now && it.member !in muted }
        voteList.value =
            groupVotes
                .mapValues { (_, byMember) -> byMember.values.filter { it.voter !in muted } }
                .filterValues { it.isNotEmpty() }
        publishGroups()
    }

    private fun publishGroups() {
        groupList.value = book.all.map { it.info() }
    }

    private fun Group?.isMutedHere(keyId: Bytes?): Boolean = this != null && isMuted(keyId)

    private fun Group.info() =
        GroupInfo(
            id = id,
            name = name,
            expiresAt = instantOfWireMinute(expiresMinute),
            myNickname = myNickname,
            members = roster.map { it.nickname },
            roster =
                roster.map {
                    MemberInfo(
                        id = it.keyId,
                        nickname = it.nickname,
                        isMe = it.keyId == self.keyId,
                        muted = isMuted(it.keyId),
                        lastSeen = seen[id to it.keyId]?.takeIf { at -> clock() - at <= SEEN_FOR },
                    )
                },
        )
}
