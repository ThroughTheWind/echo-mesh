package io.github.throughthewind.echo.core.mesh.relay

import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.TransportEvent
import io.github.throughthewind.echo.core.mesh.diagnostics.FieldDiagnostics
import io.github.throughthewind.echo.core.mesh.report.ClockCheck
import io.github.throughthewind.echo.core.mesh.report.FrameRejection
import io.github.throughthewind.echo.core.mesh.report.FrameValidator
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.instantOfWireMinute
import io.github.throughthewind.echo.core.mesh.report.wireMinute
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.FrameKind
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.JoinFrame
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.SummaryFrame
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * How the relay spends a phone's radio and battery. First guesses, tuned in the field test (WI-028);
 * the threat model (§5.8) sets their shape, not their values.
 */
data class RelayConfig(
    /** Hops a new frame of this phone's may travel (FR-5.2). */
    val hopBudget: Int = DEFAULT_HOP_BUDGET,
    /** Frames held at once (FR-5.3); a summary covers this many ids with few false positives. */
    val capacity: Int = 256,
    /** How often each neighbour gets a summary, with ±50% jitter (FR-5.1). */
    val summaryEvery: Duration = 15.seconds,
    /** Bytes pushed back in answer to one summary (FR-5.1's per-connection budget). */
    val answerBudgetBytes: Int = 16 * 1024,
    val linkFramesPerSecond: Double = 50.0,
    val linkFrameBurst: Double = 200.0,
    /** Signature checks one link may cost a second (threat model §5.8). */
    val linkVerifiesPerSecond: Double = 30.0,
    val linkVerifyBurst: Double = 150.0,
    val globalFramesPerSecond: Double = 500.0,
    val globalFrameBurst: Double = 2_000.0,
    val sweepEvery: Duration = 30.seconds,
    /** How far back distinct peers are counted for the low-density state (FR-16.1; tuned in WI-028). */
    val densityWindow: Duration = 5.minutes,
    /** Most severe first: the order a summary is answered in after recency (FR-5.1). */
    val severity: List<ReportType> = DEFAULT_SEVERITY,
) {
    init {
        require(hopBudget in 0..Wire.MAX_HOP_BUDGET) { "hop budget out of range: $hopBudget" }
        require(severity.toSet() == ReportType.entries.toSet()) { "severity must rank every report type once" }
    }

    companion object {
        const val DEFAULT_HOP_BUDGET: Int = Wire.MAX_HOP_BUDGET

        /** Danger first, then help, then the street, then all-clear and supplies. */
        val DEFAULT_SEVERITY: List<ReportType> =
            listOf(
                ReportType.TEAR_GAS,
                ReportType.KETTLE,
                ReportType.POLICE_LINE,
                ReportType.ARRESTS,
                ReportType.MEDIC,
                ReportType.STREET_CLOSED,
                ReportType.EXIT_OPEN,
                ReportType.ALL_CLEAR,
                ReportType.WATER_SUPPLIES,
            )
    }
}

/**
 * Epidemic gossip with anti-entropy (spec FR-5.1 to FR-5.3, threat model §5.4 and §5.8, ADR-0008).
 *
 * - **Verify, then store, then relay.** Every frame goes through [FrameValidator]; only a valid one
 *   is stored, and only a stored one is passed on, to every other neighbour, with one hop less.
 * - **Anti-entropy.** On meeting a neighbour, and every [RelayConfig.summaryEvery] after, a phone
 *   sends a [SummaryFrame] of what it holds; the neighbour pushes back what is missing, withdrawals
 *   first, then newest, then most severe, within [RelayConfig.answerBudgetBytes].
 * - **Budgets.** Each link has a frame budget and a signature-check budget, and all links share a
 *   global one; over budget, a frame is dropped, never queued. Storage is bounded ([ReportStore]).
 * - **Expiry.** Frames are dropped at their signed expiry plus the tolerated skew and never relayed
 *   after (FR-7.1). Nothing touches disk (FR-12.2).
 *
 * Rejected frames are dropped silently and never answered (§5.4).
 */
class Relay(
    private val transport: MeshTransport,
    private val clock: () -> Instant,
    private val random: Random,
    val config: RelayConfig = RelayConfig(),
    val clockCheck: ClockCheck = ClockCheck(),
) {
    /** What happened to frames, for tests and the diagnostics screen (WI-036). Counts only. */
    data class Stats(
        val received: Int = 0,
        val overLinkBudget: Int = 0,
        val overGlobalBudget: Int = 0,
        val rejected: Map<FrameRejection, Int> = emptyMap(),
        val stored: Int = 0,
        val forwarded: Int = 0,
        val summariesSent: Int = 0,
        val answered: Int = 0,
    )

    private class Link(
        nowMillis: Long,
        config: RelayConfig,
    ) {
        val frames = TokenBucket(config.linkFramesPerSecond, config.linkFrameBurst, nowMillis)
        val verifies = TokenBucket(config.linkVerifiesPerSecond, config.linkVerifyBurst, nowMillis)
        var lastAnswerMillis = Long.MIN_VALUE
    }

    private val mutex = Mutex()
    private val store = ReportStore(config.capacity)
    private val links = LinkedHashMap<PeerHandle, Link>()
    private var global: TokenBucket? = null
    private var stats = Stats()

    private val shown = MutableStateFlow<List<HeldFrame>>(emptyList())

    /** Public reports to show: live and not withdrawn. Updated as frames arrive and expire. */
    val reports: StateFlow<List<HeldFrame>> = shown.asStateFlow()

    private val heldVotes = MutableStateFlow<List<HeldFrame>>(emptyList())

    /** Public votes held, for the tallies (FR-6.2). Updated with [reports]. */
    val votes: StateFlow<List<HeldFrame>> = heldVotes.asStateFlow()

    private fun publishHeld(minute: Long) {
        shown.value = store.reports(minute)
        heldVotes.value = store.votes(minute)
    }

    /** A group member confirmed the public report [id] at [minute]: this phone keeps it longer (FR-6.3). */
    suspend fun groupConfirmed(
        id: Bytes,
        minute: Long,
    ) {
        mutex.withLock {
            store.groupConfirmed(id, minute)
            publishHeld(clock().wireMinute())
        }
    }

    private val inRange = MutableStateFlow(0)

    /**
     * Phones in range, approximately: peer handles seen and not yet lost (spec FR-3.2). A count only;
     * handles rotate, so it may over-count, and the UI says "nearby", not "connected".
     */
    val nearby: StateFlow<Int> = inRange.asStateFlow()

    // Peer handles heard in the last densityWindow, with when, to count them (FR-16.1). In memory,
    // for that window only; never stored, sent or shown.
    private val heard = LinkedHashMap<PeerHandle, Long>()
    private val recent = MutableStateFlow(0)

    /** Distinct peer handles heard in the last [RelayConfig.densityWindow] (FR-16.1). Rotation may over-count. */
    val recentPeers: StateFlow<Int> = recent.asStateFlow()

    private fun heardFrom(
        peer: PeerHandle,
        millis: Long,
    ) {
        heard.remove(peer)
        heard[peer] = millis
        forgetHeard(millis)
    }

    private fun forgetHeard(millis: Long) {
        val since = millis - config.densityWindow.inWholeMilliseconds
        val old = heard.entries.iterator()
        while (old.hasNext() && old.next().value < since) old.remove()
        recent.value = heard.size
    }

    private val lastArrival = MutableStateFlow<Instant?>(null)

    /** When a new report from another phone was last stored, for the mesh status (FR-3.2). */
    val lastReportReceived: StateFlow<Instant?> = lastArrival.asStateFlow()

    suspend fun stats(): Stats = mutex.withLock { stats }

    /** Field diagnostics, when the user turned them on (FR-17.1); counts only, in memory. */
    @Volatile
    var diagnostics: FieldDiagnostics? = null

    private val groupArrivals = MutableSharedFlow<HeldFrame>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Every new group frame this phone stores, from the air or its own, for the groups layer to
     * try opening. Relaying does not wait for it: members and non-members relay alike (FR-5.4).
     */
    val groupFrames: SharedFlow<HeldFrame> = groupArrivals.asSharedFlow()

    private val joinArrivals =
        MutableSharedFlow<Pair<PeerHandle, JoinFrame>>(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Join steps from phones in direct range (ADR-0009). Never stored, never relayed. */
    val joinFrames: SharedFlow<Pair<PeerHandle, JoinFrame>> = joinArrivals.asSharedFlow()

    /** Group frames held now, for a group just joined to open what is already around. */
    suspend fun heldGroupFrames(): List<HeldFrame> = mutex.withLock { store.groupFrames(clock().wireMinute()) }

    /**
     * Sends a join step to [peer], or to every phone in direct range when null (the joiner does not
     * know which neighbour showed the code). One hop only, never relayed (ADR-0009).
     */
    suspend fun sendJoin(
        peer: PeerHandle?,
        frame: JoinFrame,
    ) {
        val bytes = WireCodec.encode(frame)
        val targets = if (peer != null) listOf(peer) else mutex.withLock { links.keys.toList() }
        transmit(targets.map { it to bytes })
    }

    /** True when this phone holds the frame [id], live or withdrawn. */
    suspend fun holds(id: Bytes): Boolean = mutex.withLock { id in store }

    /**
     * Runs the relay in [scope]: handles the transport's events, sends summaries and sweeps expired
     * frames. The transport is started and stopped by its owner. Cancelling the job forgets every
     * neighbour; the frames held stay until they expire.
     */
    fun run(scope: CoroutineScope): Job =
        scope.launch {
            try {
                coroutineScope {
                    launch { transport.events.collect { handle(it) } }
                    launch {
                        while (isActive) {
                            delay(jittered(config.summaryEvery))
                            sendSummaries()
                        }
                    }
                    launch {
                        while (isActive) {
                            delay(config.sweepEvery)
                            sweep()
                        }
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        links.clear()
                        inRange.value = 0
                        heard.clear()
                        recent.value = 0
                    }
                }
            }
        }

    /**
     * Sends a frame this phone made (a report or a withdrawal from `ReportAuthor`) after checking it
     * like any other. Returns the validation, so a caller can tell it was refused.
     */
    suspend fun publish(frame: ByteArray): Validation {
        val now = clock()
        val (validation, sends) =
            mutex.withLock {
                val minute = now.wireMinute()
                val v = FrameValidator.validate(frame, minute, transport.capabilities.maxFrameBytes, seen = { it in store })
                v to
                    when (v) {
                        is Validation.Valid -> accept(HeldFrame(v.id, v.frame, v.received, null), minute)
                        is Validation.Opaque -> accept(HeldFrame(v, null, minute), minute)
                        is Validation.Rejected -> emptyList()
                    }
            }
        transmit(sends)
        return validation
    }

    /** Drops expired frames now; [run] also does it every [RelayConfig.sweepEvery]. */
    suspend fun sweep() {
        mutex.withLock {
            val now = clock()
            val minute = now.wireMinute()
            store.sweep(minute)
            publishHeld(minute)
            forgetHeard(now.toEpochMilliseconds())
        }
    }

    private suspend fun handle(event: TransportEvent) {
        when (event) {
            is TransportEvent.PeerSeen -> {
                diagnostics?.peer(event.peer, clock())
                val summary =
                    mutex.withLock {
                        links.getOrPut(event.peer) { Link(clock().toEpochMilliseconds(), config) }
                        heardFrom(event.peer, clock().toEpochMilliseconds())
                        inRange.value = links.size
                        summaryFrame(clock().wireMinute())
                    }
                transmit(listOf(event.peer to summary))
            }

            is TransportEvent.PeerLost -> {
                mutex.withLock {
                    links.remove(event.peer)
                    inRange.value = links.size
                }
            }

            is TransportEvent.FrameReceived -> {
                diagnostics?.peer(event.from, clock())
                transmit(mutex.withLock { received(event.from, event.frame) })
            }

            is TransportEvent.LinkFailed -> {
                diagnostics?.failed(event.kind, clock())
            }
        }
    }

    private fun received(
        from: PeerHandle,
        bytes: ByteArray,
    ): List<Pair<PeerHandle, ByteArray>> {
        val now = clock()
        val millis = now.toEpochMilliseconds()
        val minute = now.wireMinute()
        stats = stats.copy(received = stats.received + 1)
        heardFrom(from, millis)
        val link = links.getOrPut(from) { Link(millis, config) }
        inRange.value = links.size
        // The link's own budget first, so a flooder spends its budget, not everyone's.
        if (!link.frames.tryTake(millis)) {
            stats = stats.copy(overLinkBudget = stats.overLinkBudget + 1)
            return emptyList()
        }
        val global = global ?: TokenBucket(config.globalFramesPerSecond, config.globalFrameBurst, millis).also { global = it }
        if (!global.tryTake(millis)) {
            stats = stats.copy(overGlobalBudget = stats.overGlobalBudget + 1)
            return emptyList()
        }
        if (bytes.size == FrameKind.SUMMARY.size && (bytes[1].toInt() and 0xFF) == FrameKind.SUMMARY.code) {
            return answer(from, link, bytes, millis, minute)
        }
        if (bytes.size == FrameKind.JOIN.size && (bytes[1].toInt() and 0xFF) == FrameKind.JOIN.code) {
            val join = (WireCodec.decode(bytes, transport.capabilities.maxFrameBytes) as? DecodeResult.Accepted)?.frame as? JoinFrame
            if (join == null) reject(FrameRejection.MALFORMED) else joinArrivals.tryEmit(from to join)
            return emptyList()
        }
        val validation =
            FrameValidator.validate(
                bytes,
                minute,
                transport.capabilities.maxFrameBytes,
                seen = { it in store },
                mayVerify = { link.verifies.tryTake(millis) },
            )
        return when (validation) {
            is Validation.Rejected -> {
                val reason = validation.reason
                reject(reason)
                if (reason == FrameRejection.FROM_THE_FUTURE || reason == FrameRejection.EXPIRED) {
                    validation.createdMinute?.let { clockCheck.observe(it, minute) }
                }
                emptyList()
            }

            is Validation.Valid -> {
                clockCheck.observe(validation.frame.createdMinute, minute)
                if (validation.frame is PublicReportFrame) {
                    lastArrival.value = now
                    diagnostics?.received(now, instantOfWireMinute(validation.frame.createdMinute))
                }
                accept(HeldFrame(validation.id, validation.frame, validation.received, from), minute)
            }

            is Validation.Opaque -> {
                // A group frame: members and others relay it alike, and its creation time is sealed.
                diagnostics?.received(now, null)
                accept(HeldFrame(validation, from, minute), minute)
            }
        }
    }

    /** Stores a valid frame and returns where to pass it on: every other neighbour, one hop less. */
    private fun accept(
        held: HeldFrame,
        nowMinute: Long,
    ): List<Pair<PeerHandle, ByteArray>> {
        val link = held.link
        store.put(held)
        stats = stats.copy(stored = stats.stored + 1)
        publishHeld(nowMinute)
        if (held.frame is GroupFrame && held.id in store) groupArrivals.tryEmit(held)
        if (held.withdrawn || held.id !in store) return emptyList()
        val onward = onward(held) ?: return emptyList()
        val targets = links.keys.filter { it != link }
        stats = stats.copy(forwarded = stats.forwarded + targets.size)
        if (link != null) diagnostics?.relayed(clock(), targets.size)
        return targets.map { it to onward }
    }

    /** Answers a neighbour's summary with what it lacks (FR-5.1). */
    private fun answer(
        from: PeerHandle,
        link: Link,
        bytes: ByteArray,
        millis: Long,
        minute: Long,
    ): List<Pair<PeerHandle, ByteArray>> {
        val summary =
            (WireCodec.decode(bytes, transport.capabilities.maxFrameBytes) as? DecodeResult.Accepted)?.frame as? SummaryFrame
                ?: return emptyList<Pair<PeerHandle, ByteArray>>().also { reject(FrameRejection.MALFORMED) }
        // At most one answer per half summary period per link: answering costs this phone the most.
        val gap = config.summaryEvery.inWholeMilliseconds / 2
        if (link.lastAnswerMillis != Long.MIN_VALUE && millis - link.lastAnswerMillis < gap) return emptyList()
        link.lastAnswerMillis = millis
        val missing =
            store
                .live(minute)
                .filter { !it.withdrawn && !Summary.mayContain(summary, it.id) }
                .sortedWith(answerOrder())
        val out = mutableListOf<Pair<PeerHandle, ByteArray>>()
        var budget = config.answerBudgetBytes
        for (held in missing) {
            val onward = onward(held) ?: continue
            if (onward.size > budget) break
            budget -= onward.size
            out += from to onward
        }
        stats = stats.copy(answered = stats.answered + out.size)
        diagnostics?.relayed(clock(), out.size)
        return out
    }

    private fun answerOrder(): Comparator<HeldFrame> =
        compareBy<HeldFrame> { if (it.frame is WithdrawalFrame) 0 else 1 }
            .thenByDescending { it.sortMinute }
            .thenBy { (it.frame as? PublicReportFrame)?.let { r -> config.severity.indexOf(r.type) } ?: config.severity.size }

    /**
     * The frame to pass on, or null when it may not travel further. A frame's hop budget is how many
     * more hops it may take after the phone holding it, so this phone's own frames leave unchanged
     * and a relayed one leaves with one hop less (FR-5.2).
     */
    private fun onward(held: HeldFrame): ByteArray? {
        if (held.link == null) return held.received.toByteArray()
        val hops = held.frame.hopBudget
        if (hops <= 0) return null
        return held.received.toByteArray().also { it[Wire.HOP_BUDGET_OFFSET] = (hops - 1).toByte() }
    }

    private suspend fun sendSummaries() {
        val sends =
            mutex.withLock {
                val minute = clock().wireMinute()
                links.keys.map { it to summaryFrame(minute) }
            }
        transmit(sends)
    }

    /** A fresh summary for one neighbour: a new salt every time, so no two can be compared. */
    private fun summaryFrame(minute: Long): ByteArray {
        stats = stats.copy(summariesSent = stats.summariesSent + 1)
        return WireCodec.encode(Summary.of(store.live(minute).map { it.id }, random))
    }

    private fun reject(reason: FrameRejection) {
        stats = stats.copy(rejected = stats.rejected + (reason to (stats.rejected[reason] ?: 0) + 1))
    }

    private suspend fun transmit(sends: List<Pair<PeerHandle, ByteArray>>) {
        for ((peer, frame) in sends) transport.send(peer, frame)
    }

    private fun jittered(period: Duration): Duration = period * (0.5 + random.nextDouble())
}
