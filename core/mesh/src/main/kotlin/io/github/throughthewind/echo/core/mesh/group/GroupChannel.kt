package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.mesh.relay.HeldFrame
import io.github.throughthewind.echo.core.mesh.relay.Relay
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.WireUnits
import io.github.throughthewind.echo.core.mesh.report.instantOfWireMinute
import io.github.throughthewind.echo.core.mesh.report.wireMinute
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.Report
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A group as the screens show it: never its keys. */
data class GroupInfo(
    val id: Bytes,
    val name: String,
    val expiresAt: Instant,
    val myNickname: String,
    /** Nicknames of the members this phone knows of, itself included (FR-8.7). */
    val members: List<String>,
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

/**
 * This phone's groups on the mesh (WI-018): it seals the user's group messages and hands them to the
 * relay like any frame, and opens every group frame the relay stores, from the air or its own.
 * Everyone relays group frames (FR-5.4); only this layer reads them, and only for groups it is in.
 */
class GroupChannel(
    private val relay: Relay,
    private val clock: () -> Instant,
    private val hopBudget: Int,
) {
    private val lock = Mutex()
    private val book = GroupBook()
    private val held = LinkedHashMap<Bytes, GroupReport>()

    /** Who sent each held report, by member key id: only they may withdraw it. */
    private val authors = HashMap<Bytes, Bytes>()

    /** Withdrawals seen, target id to the key id that signed them, even before the report arrives. */
    private val withdrawals = HashMap<Bytes, Bytes>()

    private val groupList = MutableStateFlow<List<GroupInfo>>(emptyList())
    private val shown = MutableStateFlow<List<GroupReport>>(emptyList())

    val groups: StateFlow<List<GroupInfo>> = groupList.asStateFlow()

    /** Live, not withdrawn group reports from every group this phone is in. */
    val reports: StateFlow<List<GroupReport>> = shown.asStateFlow()

    /** Opens group frames as the relay stores them. */
    fun run(scope: CoroutineScope): Job = scope.launch { relay.groupFrames.collect { open(it) } }

    /** Creates a group (FR-8.1). Null when this phone is already in five. */
    suspend fun create(
        name: String,
        nickname: String,
        lifetime: Duration = GroupBook.DEFAULT_LIFETIME,
    ): GroupInfo? =
        lock.withLock {
            book.create(name, lifetime, nickname, clock().wireMinute())?.also { publishGroups() }?.info()
        }

    /** Leaves [id] (FR-8.5): its keys go, and so do its reports on this phone. */
    suspend fun leave(id: Bytes) {
        lock.withLock {
            book.leave(id)
            held.values.removeAll { it.groupId == id }
            authors.keys.retainAll(held.keys)
            publish()
        }
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

    /** Ratchets keys, ends expired groups (FR-8.6) and drops expired reports. */
    suspend fun sweep() {
        lock.withLock {
            val now = clock()
            book.sweep(now.wireMinute())
            held.values.removeAll { it.report.isExpired(now) || book[it.groupId] == null }
            authors.keys.retainAll(held.keys)
            if (withdrawals.size > MAX_WITHDRAWALS) withdrawals.clear()
            publish()
        }
    }

    /** Opens what the relay already holds, for a group just created or joined. */
    suspend fun rescan() {
        relay.heldGroupFrames().forEach { open(it) }
    }

    internal suspend fun <T> withBook(action: (GroupBook) -> T): T =
        lock.withLock {
            val result = action(book)
            publishGroups()
            result
        }

    private suspend fun open(frame: HeldFrame) {
        val group = frame.frame as? GroupFrame ?: return
        lock.withLock {
            if (frame.id in held) return
            val opened = book.open(group) ?: return
            when (val message = opened.message) {
                is GroupMessage.Report -> {
                    val report =
                        Report(
                            type = message.type,
                            position = GeoPoint(WireUnits.degrees(message.latitude), WireUnits.degrees(message.longitude)),
                            createdAt = instantOfWireMinute(message.createdMinute),
                            lifetime = message.lifetimeMinutes.minutes,
                        )
                    if (report.isExpired(clock()) || withdrawals[frame.id] == opened.sender.keyId) return
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

                is GroupMessage.Withdrawal -> {
                    // Only the member who sent a report can withdraw it.
                    withdrawals[message.target] = opened.sender.keyId
                    if (authors[message.target] == opened.sender.keyId) {
                        held.remove(message.target)
                        authors.remove(message.target)
                    }
                }

                is GroupMessage.MemberAdded -> {
                    Unit
                }
            }
            publish()
        }
    }

    private companion object {
        /** Withdrawals remembered for reports not seen yet; bounded like everything a peer can fill. */
        const val MAX_WITHDRAWALS = 512
    }

    private fun publish() {
        val now = clock()
        shown.value = held.values.filter { !it.report.isExpired(now) }
        publishGroups()
    }

    private fun publishGroups() {
        groupList.value = book.all.map { it.info() }
    }

    private fun Group.info() =
        GroupInfo(
            id = id,
            name = name,
            expiresAt = instantOfWireMinute(expiresMinute),
            myNickname = myNickname,
            members = roster.map { it.nickname },
        )
}
