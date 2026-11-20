package io.github.throughthewind.echo.core.mesh.relay

import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.report.FrameValidator
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.VoteRule
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Frame
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.PublicVoteFrame
import io.github.throughthewind.echo.core.mesh.wire.SignedFrame
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame

/** A verified frame, or a group frame only members can check, that this phone holds, in memory only (spec FR-12.2). */
class HeldFrame internal constructor(
    val id: Bytes,
    val frame: Frame,
    /** The frame as it arrived; its hop budget is what this phone may still pass on. */
    val received: Bytes,
    /** The link it came over, for fair eviction; null for this phone's own frames. Never stored. */
    internal val link: PeerHandle?,
    /** Its signed creation minute, or for a group frame the minute it was first seen: for ordering. */
    val sortMinute: Long,
    /**
     * The minute it is dropped: its signed expiry plus the tolerated skew (threat model §5.2), or
     * for a group frame, whose expiry only members can read, [GROUP_HOLD_MINUTES] after first seen.
     */
    dropMinute: Long,
) {
    /** When it is dropped; a public report's moves later as confirmations arrive (FR-6.3). */
    var dropMinute: Long = dropMinute
        internal set

    internal constructor(
        id: Bytes,
        frame: SignedFrame,
        received: Bytes,
        link: PeerHandle?,
    ) : this(
        id,
        frame,
        received,
        link,
        frame.createdMinute,
        frame.createdMinute + frame.lifetimeMinutes + FrameValidator.EXPIRY_GRACE_MINUTES,
    )

    internal constructor(
        opaque: Validation.Opaque,
        link: PeerHandle?,
        nowMinute: Long,
    ) : this(opaque.id, opaque.frame, opaque.received, link, nowMinute, nowMinute + GROUP_HOLD_MINUTES)

    companion object {
        /** The longest life any frame can claim, plus clock skew (ADR-0009). */
        const val GROUP_HOLD_MINUTES: Long = 185
    }

    /** A report its author withdrew (FR-4.5): kept, so its id is still known, but never shown or relayed. */
    var withdrawn: Boolean = false
        internal set

    /** Hops it may still travel from this phone. */
    val hopsLeft: Int get() = frame.hopBudget
}

/**
 * The frames a phone holds, bounded (spec FR-5.3, threat model §5.8). Not thread-safe: the relay
 * owns it and serialises access.
 *
 * When full, it evicts from the link that brought the most frames, oldest first, so a flooding peer
 * pushes out its own frames rather than everyone else's. This phone's own frames are never evicted.
 */
class ReportStore(
    val capacity: Int,
) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val held = LinkedHashMap<Bytes, HeldFrame>()

    val size: Int get() = held.size

    operator fun contains(id: Bytes): Boolean = id in held

    operator fun get(id: Bytes): HeldFrame? = held[id]

    /** Adds a verified frame. Returns the frames evicted to make room. */
    internal fun put(frame: HeldFrame): List<HeldFrame> {
        if (frame.id in held) return emptyList()
        when (val f = frame.frame) {
            is WithdrawalFrame -> {
                val target = held[f.target]
                val report = target?.frame
                if (report is PublicReportFrame && FrameValidator.withdraws(f, report, target.id)) target.withdrawn = true
            }

            is PublicReportFrame -> {
                frame.withdrawn = held.values.any { w -> w.frame is WithdrawalFrame && FrameValidator.withdraws(w.frame, f, frame.id) }
                held.values.forEach { v ->
                    val vote = v.frame
                    if (vote is PublicVoteFrame && vote.target == frame.id &&
                        vote.vote == Vote.CONFIRM
                    ) {
                        confirm(frame, f, vote.createdMinute)
                    }
                }
            }

            is PublicVoteFrame -> {
                val target = held[f.target]
                val report = target?.frame
                if (report is PublicReportFrame && f.vote == Vote.CONFIRM) confirm(target, report, f.createdMinute)
            }

            else -> {}
        }
        held[frame.id] = frame
        val evicted = mutableListOf<HeldFrame>()
        while (held.size > capacity) {
            val victim = victim() ?: break
            held.remove(victim.id)
            evicted += victim
        }
        return evicted
    }

    private fun victim(): HeldFrame? {
        val byLink = held.values.filter { it.link != null }.groupBy { it.link }
        val busiest = byLink.values.maxByOrNull { it.size } ?: return null
        return busiest.minBy { it.sortMinute }
    }

    /** Drops what has expired at [nowMinute] (FR-7.1). Returns how many were dropped. */
    fun sweep(nowMinute: Long): Int {
        val before = held.size
        held.values.removeAll { nowMinute >= it.dropMinute }
        return before - held.size
    }

    /** Every frame still live at [nowMinute], withdrawn reports included. */
    fun live(nowMinute: Long): List<HeldFrame> = held.values.filter { nowMinute < it.dropMinute }

    /** Public votes still held at [nowMinute], for the tallies (FR-6.2). */
    fun votes(nowMinute: Long): List<HeldFrame> = live(nowMinute).filter { it.frame is PublicVoteFrame }

    /**
     * Keeps the report [id] until at least [untilMinute] plus the tolerated skew: a group member's
     * confirmation, which only members can read, refreshes it on their phones (FR-6.3).
     */
    fun extend(
        id: Bytes,
        untilMinute: Long,
    ) {
        val target = held[id] ?: return
        target.dropMinute = maxOf(target.dropMinute, untilMinute + FrameValidator.EXPIRY_GRACE_MINUTES)
    }

    /** A group member confirmed the public report [id] at [minute]: it lives longer here too (FR-6.3). */
    fun groupConfirmed(
        id: Bytes,
        minute: Long,
    ) {
        val target = held[id] ?: return
        val report = target.frame as? PublicReportFrame ?: return
        confirm(target, report, minute)
    }

    private fun confirm(
        target: HeldFrame,
        report: PublicReportFrame,
        confirmMinute: Long,
    ) {
        val until =
            VoteRule.extendedBy(
                report.createdMinute,
                report.lifetimeMinutes,
                report.type.defaultLifetime.inWholeMinutes
                    .toInt(),
                confirmMinute,
            )
        target.dropMinute = maxOf(target.dropMinute, until + FrameValidator.EXPIRY_GRACE_MINUTES)
    }

    /** Public reports to show at [nowMinute]: live and not withdrawn. */
    fun reports(nowMinute: Long): List<HeldFrame> = live(nowMinute).filter { it.frame is PublicReportFrame && !it.withdrawn }

    /** Group frames still held at [nowMinute], for members to open (FR-5.4). */
    fun groupFrames(nowMinute: Long): List<HeldFrame> = live(nowMinute).filter { it.frame is GroupFrame }
}
