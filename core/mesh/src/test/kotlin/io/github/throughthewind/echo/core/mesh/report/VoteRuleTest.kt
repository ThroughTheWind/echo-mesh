package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Vote
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Every branch of FR-6.3 and FR-6.4 (WI-019, threat model §5.7). */
class VoteRuleTest {
    private val created = 1_000L
    private val lifetime = 30
    private val default = 30

    private fun voter(n: Int) = Bytes(ByteArray(8) { n.toByte() })

    private fun group(
        n: Int,
        vote: Vote,
        minute: Long,
    ) = CastVote(voter(n), fromGroup = true, vote = vote, minute = minute)

    private fun nearby(
        n: Int,
        vote: Vote,
        minute: Long = created + 1,
    ) = CastVote(voter(100 + n), fromGroup = false, vote = vote, minute = minute)

    private fun tally(vararg votes: CastVote) = VoteRule.tally(votes.toList(), created, lifetime, default)

    @Test
    fun `no votes, no change`() {
        val t = tally()
        assertEquals(Tally(expiresMinute = created + lifetime), t)
    }

    @Test
    fun `a voter counts once, by their latest vote`() {
        val t =
            tally(
                nearby(1, Vote.CONFIRM, created + 1),
                nearby(1, Vote.DISMISS, created + 5),
                group(1, Vote.DISMISS, created + 2),
                group(
                    1,
                    Vote.CONFIRM,
                    created + 3,
                ),
            )
        assertEquals(0, t.nearbyConfirmed)
        assertEquals(1, t.nearbyDismissed)
        assertEquals(1, t.groupConfirmed)
        assertEquals(0, t.groupDismissed)
    }

    @Test
    fun `(a) a group dismissal fades the pin until a group member confirms at or after it`() {
        assertTrue(tally(group(1, Vote.DISMISS, created + 5)).probablyGone)
        assertTrue(tally(group(1, Vote.DISMISS, created + 5)).fadedByGroup)
        assertTrue(tally(group(2, Vote.CONFIRM, created + 4), group(1, Vote.DISMISS, created + 5)).probablyGone, "confirmed before")
        assertFalse(tally(group(1, Vote.DISMISS, created + 5), group(2, Vote.CONFIRM, created + 5)).probablyGone, "same minute")
        assertFalse(tally(group(1, Vote.DISMISS, created + 5), group(2, Vote.CONFIRM, created + 9)).probablyGone, "confirmed after")
        assertTrue(
            tally(
                group(1, Vote.DISMISS, created + 5),
                group(2, Vote.CONFIRM, created + 6),
                group(3, Vote.DISMISS, created + 8),
            ).probablyGone,
            "a later dismissal than the last confirmation",
        )
    }

    @Test
    fun `(b) public dismissals fade a pin only at 3 or more and twice the confirmations`() {
        assertFalse(tally(nearby(1, Vote.DISMISS), nearby(2, Vote.DISMISS)).probablyGone, "two are not enough")
        assertTrue(tally(nearby(1, Vote.DISMISS), nearby(2, Vote.DISMISS), nearby(3, Vote.DISMISS)).probablyGone)
        assertTrue(
            tally(
                nearby(1, Vote.DISMISS),
                nearby(2, Vote.DISMISS),
                nearby(3, Vote.DISMISS),
                nearby(4, Vote.DISMISS),
                nearby(5, Vote.CONFIRM),
                nearby(6, Vote.CONFIRM),
            ).probablyGone,
            "4 against 2 is twice",
        )
        assertFalse(
            tally(
                nearby(1, Vote.DISMISS),
                nearby(2, Vote.DISMISS),
                nearby(3, Vote.DISMISS),
                nearby(4, Vote.CONFIRM),
                nearby(5, Vote.CONFIRM),
            ).probablyGone,
            "3 against 2 is not",
        )
    }

    @Test
    fun `(b) never fades a report a group member confirmed, whatever the public says`() {
        val flood = (1..50).map { nearby(it, Vote.DISMISS) }.toTypedArray()
        assertFalse(tally(*flood, group(1, Vote.CONFIRM, created)).probablyGone)
        assertTrue(tally(*flood).probablyGone)
        assertFalse(tally(*flood).fadedByGroup, "faded by the public rule")
    }

    @Test
    fun `a confirmation refreshes time left, never past twice the default from creation`() {
        assertEquals(created + 10 + lifetime, tally(nearby(1, Vote.CONFIRM, created + 10)).expiresMinute)
        assertEquals(created + 2 * default, tally(nearby(1, Vote.CONFIRM, created + 50)).expiresMinute, "capped")
        assertEquals(created + lifetime, tally(nearby(1, Vote.DISMISS, created + 10)).expiresMinute, "a dismissal never extends")
        assertEquals(
            created + lifetime,
            tally(nearby(1, Vote.CONFIRM, created - 10)).expiresMinute,
            "a vote from before creation does not shorten",
        )
        assertEquals(
            created + 20 + lifetime,
            tally(nearby(1, Vote.CONFIRM, created + 5), group(1, Vote.CONFIRM, created + 20)).expiresMinute,
        )
    }
}
