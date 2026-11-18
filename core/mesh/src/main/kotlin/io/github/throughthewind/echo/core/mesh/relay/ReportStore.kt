package io.github.throughthewind.echo.core.mesh.relay

import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.report.FrameValidator
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.SignedFrame
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame

/** A verified frame this phone holds, in memory only (spec FR-12.2). */
class HeldFrame internal constructor(
    val id: Bytes,
    val frame: SignedFrame,
    /** The frame as it arrived; its hop budget is what this phone may still pass on. */
    val received: Bytes,
    /** The link it came over, for fair eviction; null for this phone's own frames. Never stored. */
    internal val link: PeerHandle?,
) {
    /** The minute it is dropped: its signed expiry plus the tolerated skew (threat model §5.2). */
    val dropMinute: Long get() = frame.createdMinute + frame.lifetimeMinutes + FrameValidator.EXPIRY_GRACE_MINUTES

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
        return busiest.minBy { it.frame.createdMinute }
    }

    /** Drops what has expired at [nowMinute] (FR-7.1). Returns how many were dropped. */
    fun sweep(nowMinute: Long): Int {
        val before = held.size
        held.values.removeAll { nowMinute >= it.dropMinute }
        return before - held.size
    }

    /** Every frame still live at [nowMinute], withdrawn reports included. */
    fun live(nowMinute: Long): List<HeldFrame> = held.values.filter { nowMinute < it.dropMinute }

    /** Public reports to show at [nowMinute]: live and not withdrawn. */
    fun reports(nowMinute: Long): List<HeldFrame> = live(nowMinute).filter { it.frame is PublicReportFrame && !it.withdrawn }
}
