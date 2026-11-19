package io.github.throughthewind.echo.core.mesh.node

import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.TransportUnavailableException
import io.github.throughthewind.echo.core.mesh.group.GroupChannel
import io.github.throughthewind.echo.core.mesh.group.GroupInfo
import io.github.throughthewind.echo.core.mesh.relay.Relay
import io.github.throughthewind.echo.core.mesh.relay.RelayConfig
import io.github.throughthewind.echo.core.mesh.report.ReportAuthor
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.toReport
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.Report
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A report as the screens show it (spec FR-5.5): what it says, how far it came, and whether it is ours. */
data class ShownReport(
    val id: Bytes,
    val report: Report,
    /** Hops it travelled to reach this phone, 0 when it arrived directly or is ours. Approximate. */
    val hops: Int,
    /** Sent from this phone, so it can be withdrawn (FR-4.5). */
    val mine: Boolean,
    /** For a group report (FR-9.2): the group's name and who in it sent it. Null for a public one. */
    val group: GroupTag? = null,
)

/** Where a group report came from, shown in the verified style (FR-9.2). */
data class GroupTag(
    val groupId: Bytes,
    val groupName: String,
    val sender: String,
)

/** Whether the mesh is on, as the mesh status shows it (spec FR-3.1, FR-3.2). */
sealed interface MeshState {
    /** Off: no radio use at all. The first-launch state (FR-3.1). */
    data object Off : MeshState

    /** This build has no transport: nothing to turn on. */
    data object NoRadio : MeshState

    /** The transport refused to start for a reason the user can see and maybe fix (FR-3.5). */
    data class Unavailable(
        val reason: io.github.throughthewind.echo.core.mesh.Unavailable,
    ) : MeshState

    /** The transport failed to start for a reason it did not name. */
    data object Failed : MeshState

    /** On since [since]: [nearby] phones in range, the last report from another phone at [lastReport]. */
    data class On(
        val since: Instant,
        val nearby: Int,
        val lastReport: Instant?,
    ) : MeshState
}

/** A report waiting out its grace period before it is signed and sent (FR-4.7). */
data class PendingReport(
    val type: ReportType,
    val position: GeoPoint,
    val sendsAt: Instant,
    /** The group it goes to, or null for everyone nearby (FR-4.3). */
    val group: Bytes? = null,
)

/**
 * This phone on the mesh: one transport, the relay that runs over it, and the user's own reports.
 * The screens' only door to the mesh. Turning it off stops the radio; reports already held stay
 * until they expire, in memory only (FR-12.2).
 */
class MeshNode(
    private val transport: MeshTransport?,
    private val scope: CoroutineScope,
    private val clock: () -> Instant,
    random: Random,
    val config: RelayConfig = RelayConfig(),
) {
    private val relay: Relay? = transport?.let { Relay(it, clock, random, config) }

    /** This phone's groups on the mesh (WI-018); null when there is no radio to reach them over. */
    val groupChannel: GroupChannel? = relay?.let { GroupChannel(it, clock, config.hopBudget) }

    /** The groups this phone is in. */
    val groups: StateFlow<List<GroupInfo>> = groupChannel?.groups ?: MutableStateFlow(emptyList<GroupInfo>()).asStateFlow()
    private val author = ReportAuthor(clock)
    private val lock = Mutex()
    private var running: Job? = null
    private val power = MutableStateFlow<MeshState>(if (transport == null) MeshState.NoRadio else MeshState.Off)
    private val ownIds = MutableStateFlow<Set<Bytes>>(emptySet())
    private val waiting = MutableStateFlow<PendingReport?>(null)
    private var grace: Job? = null

    /** On, off, or unable; with the count of phones nearby while on. */
    val state: StateFlow<MeshState> =
        if (relay == null) {
            power.asStateFlow()
        } else {
            combine(power, relay.nearby, relay.lastReportReceived) { power, nearby, last ->
                if (power is MeshState.On) power.copy(nearby = nearby, lastReport = last) else power
            }.stateIn(scope, SharingStarted.Eagerly, power.value)
        }

    /** Every live report this phone holds, its own included, public and from its groups. */
    val reports: StateFlow<List<ShownReport>> =
        if (relay == null || groupChannel == null) {
            MutableStateFlow(emptyList<ShownReport>()).asStateFlow()
        } else {
            combine(relay.reports, ownIds, groupChannel.reports) { held, own, fromGroups ->
                held.mapNotNull { frame ->
                    val report = frame.frame as? PublicReportFrame ?: return@mapNotNull null
                    ShownReport(
                        id = frame.id,
                        report = report.toReport(),
                        hops = if (frame.id in own) 0 else (config.hopBudget - frame.hopsLeft).coerceAtLeast(0),
                        mine = frame.id in own,
                    )
                } +
                    fromGroups.map {
                        ShownReport(
                            id = it.id,
                            report = it.report,
                            hops = if (it.mine) 0 else (config.hopBudget - it.hopsLeft).coerceAtLeast(0),
                            mine = it.mine,
                            group = GroupTag(it.groupId, it.groupName, it.sender),
                        )
                    }
            }.stateIn(scope, SharingStarted.Eagerly, emptyList())
        }

    /** The report in its grace period, if any. */
    val pending: StateFlow<PendingReport?> = waiting.asStateFlow()

    init {
        // Reports expire with the mesh off too.
        if (relay != null) {
            groupChannel?.run(scope)
            scope.launch {
                while (true) {
                    delay(config.sweepEvery)
                    relay.sweep()
                    groupChannel?.sweep()
                }
            }
        }
    }

    /**
     * Starts the radio and the relay. Only from a user's tap: never at boot (FR-3.3). True when the
     * mesh is on afterwards; otherwise [state] says why not.
     */
    suspend fun turnOn(): Boolean {
        val transport = transport ?: return false
        val relay = relay ?: return false
        lock.withLock {
            if (running != null) return true
            try {
                transport.start()
            } catch (e: CancellationException) {
                throw e
            } catch (e: TransportUnavailableException) {
                power.value = MeshState.Unavailable(e.reason)
                return false
            } catch (_: Exception) {
                power.value = MeshState.Failed
                return false
            }
            running = relay.run(scope)
            power.value = MeshState.On(since = clock(), nearby = 0, lastReport = null)
            return true
        }
    }

    /** Stops every radio use (FR-3.1). Held reports stay until they expire; nothing new arrives. */
    suspend fun turnOff() {
        val transport = transport ?: return
        lock.withLock {
            running?.cancel()
            running = null
            try {
                transport.stop()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Already stopped: off is off.
            }
            power.value = MeshState.Off
        }
    }

    /** When [type] may be reported again (FR-4.6), or null if now. */
    fun nextAllowed(type: ReportType): Instant? = author.nextAllowed(type)

    /**
     * Puts a report of [type] at [position] in its grace period: it is signed and sent after
     * [SEND_GRACE] unless [cancelPending] is called first (FR-4.7). It goes to everyone nearby, or
     * only to the group [group] (FR-4.3). Returns false when another report is already waiting or
     * the type is rate-limited; the rate limit counts both audiences (FR-4.6).
     */
    fun send(
        type: ReportType,
        position: GeoPoint,
        group: Bytes? = null,
    ): Boolean {
        val relay = relay ?: return false
        if (waiting.value != null || author.nextAllowed(type) != null) return false
        val pending = PendingReport(type, position, clock() + SEND_GRACE, group)
        waiting.value = pending
        grace =
            scope.launch {
                delay(SEND_GRACE)
                if (!waiting.compareAndSet(pending, null)) return@launch
                if (group == null) {
                    val outcome = author.report(type, position, config.hopBudget)
                    if (outcome is ReportAuthor.Outcome.Sent) {
                        ownIds.value = ownIds.value + outcome.id
                        relay.publish(outcome.frame)
                    }
                } else if (author.takeSlot(type)) {
                    groupChannel?.sendReport(group, type, position)
                }
            }
        return true
    }

    /** Drops the report in its grace period; nothing was signed and nothing left the phone. */
    fun cancelPending() {
        waiting.value = null
        grace?.cancel()
        grace = null
    }

    /** Withdraws one of this phone's reports everywhere it spread (FR-4.5). False if it cannot. */
    suspend fun withdraw(id: Bytes): Boolean {
        val relay = relay ?: return false
        if (groupChannel?.withdraw(id) == true) return true
        val frame = author.withdraw(id, config.hopBudget) ?: return false
        return relay.publish(frame) is Validation.Valid
    }

    companion object {
        /** Time to cancel a report before it leaves the phone (token `send-grace`, FR-4.7). */
        val SEND_GRACE: Duration = 5.seconds

        /** How long a mesh that has heard nobody yet says "Looking for phones" (components/MeshStatus). */
        val SEARCHING: Duration = 30.seconds
    }
}
