package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Vote

/**
 * One vote on a report, as this phone holds it. [voter] is what a vote counts once per (threat
 * model §5.7): a member key id for a group vote, the one-time signing key for a public one.
 */
data class CastVote(
    val voter: Bytes,
    val fromGroup: Boolean,
    val vote: Vote,
    /** Its signed creation minute. */
    val minute: Long,
)

/** What the votes on one report add up to (FR-6.2 to FR-6.4). */
data class Tally(
    val groupConfirmed: Int = 0,
    val groupDismissed: Int = 0,
    val nearbyConfirmed: Int = 0,
    val nearbyDismissed: Int = 0,
    /** FR-6.4: shown faded, never removed. */
    val probablyGone: Boolean = false,
    /** Faded by rule (a), a group member's dismissal, rather than by public votes: the detail says which. */
    val fadedByGroup: Boolean = false,
    /** When the report now expires, in minutes since the Unix epoch: confirmations refresh it (FR-6.3). */
    val expiresMinute: Long,
)

/**
 * The vote rules, one pure function each so every branch is tested (WI-019).
 *
 * - One vote per voter: a voter's latest vote counts, earlier ones do not (FR-6.1).
 * - A confirmation refreshes the report's time left to its full lifetime from the confirmation,
 *   never past twice the type's default lifetime from creation, so nothing lives forever (FR-6.3).
 * - "Probably gone" (FR-6.4, threat model §5.7): (a) a group member dismissed it and no group
 *   member confirmed it at or after that dismissal, or (b) public dismissals are at least 3 and at
 *   least twice the public confirmations, and no group member confirmed it.
 */
object VoteRule {
    /** Public dismissals needed before they can fade a pin on their own (FR-6.4 b). */
    const val PUBLIC_DISMISSALS = 3

    fun tally(
        votes: Collection<CastVote>,
        createdMinute: Long,
        lifetimeMinutes: Int,
        defaultLifetimeMinutes: Int,
    ): Tally {
        val latest =
            votes
                .groupBy { it.fromGroup to it.voter }
                .values
                .map { byVoter -> byVoter.maxBy { it.minute } }
        val group = latest.filter { it.fromGroup }
        val nearby = latest.filter { !it.fromGroup }
        val groupConfirms = group.filter { it.vote == Vote.CONFIRM }
        val groupDismisses = group.filter { it.vote == Vote.DISMISS }
        val nearbyConfirmed = nearby.count { it.vote == Vote.CONFIRM }
        val nearbyDismissed = nearby.count { it.vote == Vote.DISMISS }
        val lastGroupDismiss = groupDismisses.maxOfOrNull { it.minute }
        val ruleA = lastGroupDismiss != null && groupConfirms.none { it.minute >= lastGroupDismiss }
        val ruleB =
            nearbyDismissed >= PUBLIC_DISMISSALS &&
                nearbyDismissed >= 2 * nearbyConfirmed &&
                groupConfirms.isEmpty()
        var expires = createdMinute + lifetimeMinutes
        latest.filter { it.vote == Vote.CONFIRM }.forEach {
            expires = maxOf(expires, extendedBy(createdMinute, lifetimeMinutes, defaultLifetimeMinutes, it.minute))
        }
        return Tally(
            groupConfirmed = groupConfirms.size,
            groupDismissed = groupDismisses.size,
            nearbyConfirmed = nearbyConfirmed,
            nearbyDismissed = nearbyDismissed,
            probablyGone = ruleA || ruleB,
            fadedByGroup = ruleA,
            expiresMinute = expires,
        )
    }

    /** The expiry one confirmation at [confirmMinute] gives a report, capped (FR-6.3). */
    fun extendedBy(
        createdMinute: Long,
        lifetimeMinutes: Int,
        defaultLifetimeMinutes: Int,
        confirmMinute: Long,
    ): Long {
        val cap = createdMinute + maxOf(lifetimeMinutes, 2 * defaultLifetimeMinutes)
        val refreshed = maxOf(createdMinute, confirmMinute) + lifetimeMinutes
        return minOf(maxOf(createdMinute + lifetimeMinutes, refreshed), cap)
    }
}
