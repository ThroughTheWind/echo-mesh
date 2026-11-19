package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.crypto.GroupEpochs
import io.github.throughthewind.echo.core.crypto.MemberKey
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.model.ReportType
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class GroupBookTest {
    private val now = 29_000_000L
    private val report = GroupMessage.Report(ReportType.MEDIC, 488_566, 23_522, now, 90)

    private fun frame(bytes: ByteArray): GroupFrame {
        assertEquals(Wire.GROUP_FRAME_BYTES, bytes.size)
        return assertIs<DecodeResult.Accepted>(WireCodec.decode(bytes)).frame as GroupFrame
    }

    /** What a join hands over, done by hand: the current epoch key, the group facts and the roster. */
    private fun joined(
        book: GroupBook,
        from: Group,
        nickname: String,
    ): Group {
        val epoch = GroupBook.epochOf(now)
        val group =
            Group(
                from.id,
                from.name,
                from.expiresMinute,
                GroupEpochs.fromKey(epoch, from.epochs.keyFor(epoch)!!),
                MemberKey.generate(),
                nickname,
                from.roster,
            )
        assertTrue(book.adopt(group))
        from.add(group.self)
        return group
    }

    @Test
    fun `a member reads a group report and knows who sent it`() {
        val a = GroupBook()
        val b = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        joined(b, group, "Owl")
        val opened = assertNotNull(b.open(frame(a.seal(group, report, now, 7))))
        assertEquals(report, opened.message)
        assertEquals("Fox", opened.sender.nickname)
        assertEquals("Crew", opened.group.name)
        val back = assertNotNull(a.open(frame(b.groups().first().let { b.seal(it, report, now, 7) })))
        assertEquals("Owl", back.sender.nickname)
        val vote = GroupMessage.ReportVote(Bytes(ByteArray(32) { 5 }), Vote.DISMISS, now, 90)
        assertEquals(vote, assertNotNull(b.open(frame(a.seal(group, vote, now, 7)))).message, "a vote reads back as sent")
        val position = GroupMessage.Position(488675, 23632, 59, now, 5)
        assertEquals(position, assertNotNull(b.open(frame(a.seal(group, position, now, 7)))).message, "a position reads back as sent")
    }

    @Test
    fun `a phone outside the group cannot read it (AC-14)`() {
        val a = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        val outsider = GroupBook().apply { create("Other", 24.hours, "Cat", now) }
        assertNull(outsider.open(frame(a.seal(group, report, now, 7))))
        assertNull(GroupBook().open(frame(a.seal(group, report, now, 7))))
    }

    @Test
    fun `a frame from someone not on the roster is not shown`() {
        val a = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        val b = GroupBook()
        val epoch = GroupBook.epochOf(now)
        // B holds the key (a leaked key) but A never added B to its roster.
        val leaked =
            Group(
                group.id,
                group.name,
                group.expiresMinute,
                GroupEpochs.fromKey(epoch, group.epochs.keyFor(epoch)!!),
                MemberKey.generate(),
                "Rat",
                group.roster,
            )
        b.adopt(leaked)
        assertNull(a.open(frame(b.seal(leaked, report, now, 7))))
    }

    @Test
    fun `a member added message puts the newcomer on everyone's roster`() {
        val a = GroupBook()
        val c = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        joined(c, group, "Cat")
        val newcomer = MemberKey.generate()
        val added = GroupMessage.MemberAdded(Bytes(newcomer.publicKey), "Owl", now, 180)
        val opened = assertNotNull(c.open(frame(a.seal(group, added, now, 7))))
        assertEquals(added, opened.message)
        assertTrue(
            c
                .groups()
                .first()
                .roster
                .any { it.nickname == "Owl" },
        )
    }

    @Test
    fun `after leaving, the group's frames cannot be read (AC-16)`() {
        val a = GroupBook()
        val b = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        val mine = joined(b, group, "Owl")
        val sealed = frame(a.seal(group, report, now, 7))
        b.leave(mine.id)
        assertNull(b.open(sealed))
        assertTrue(b.all.isEmpty())
    }

    @Test
    fun `a group expires at the end of its lifetime on every phone`() {
        val a = GroupBook()
        val group = a.create("Crew", 2.hours, "Fox", now)!!
        val sealed = frame(a.seal(group, report, now, 7))
        assertTrue(a.sweep(now + 119).isEmpty())
        assertEquals(listOf(group), a.sweep(now + 120))
        assertNull(a.open(sealed))
    }

    @Test
    fun `keys move on every hour and old ones go`() {
        val a = GroupBook()
        val b = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        joined(b, group, "Owl")
        val early = frame(a.seal(group, report, now, 7))
        a.sweep(now + 60 * 3)
        b.sweep(now + 60 * 3)
        assertNotNull(b.open(early), "three hours back is still readable")
        a.sweep(now + 60 * 6)
        b.sweep(now + 60 * 6)
        assertNull(b.open(early), "six hours back is gone")
        assertNotNull(b.open(frame(a.seal(group, report, now + 60 * 6, 7))))
    }

    @Test
    fun `a phone is in at most five groups`() {
        val a = GroupBook()
        repeat(GroupBook.MAX_GROUPS) { assertNotNull(a.create("G$it", 24.hours, "Fox", now)) }
        assertNull(a.create("One more", 24.hours, "Fox", now))
    }

    @Test
    fun `every group frame is the same size whatever it says`() {
        val a = GroupBook()
        val group = a.create("Crew", 24.hours, "Fox", now)!!
        val sizes =
            listOf(
                report,
                GroupMessage.Withdrawal(Bytes(ByteArray(32)), now, 90),
                GroupMessage.ReportVote(Bytes(ByteArray(32)), Vote.CONFIRM, now, 90),
                GroupMessage.Position(488675, 23632, 0, now, 5),
                GroupMessage.MemberAdded(Bytes(ByteArray(32) { 1 }), "A much longer nickname here", now, 180),
            ).map { a.seal(group, it, now, 7).size }
        assertEquals(setOf(Wire.GROUP_FRAME_BYTES), sizes.toSet())
    }

    @Test
    fun `the inner decoder never throws and refuses anything but one exact message`() {
        val random = Random(3)
        repeat(2_000) { GroupInner.decode(random.nextBytes(GroupInner.SIZE)) }
        repeat(50) { GroupInner.decode(random.nextBytes(random.nextInt(0, 300))) }
        val key = MemberKey.generate()
        val good = GroupInner.encode(Bytes(ByteArray(16)), 1, key, report)
        assertNotNull(GroupInner.decode(good))
        val padded = good.copyOf().also { it[GroupInner.SIGNED_BYTES - 1] = 1 }
        assertNull(GroupInner.decode(padded), "padding must be zero")
        val badType = good.copyOf().also { it[GroupInner.KEY_ID_BYTES] = 0x7F }
        assertNull(GroupInner.decode(badType))
        val position = GroupInner.encode(Bytes(ByteArray(16)), 1, key, GroupMessage.Position(488675, 23632, 59, now, 5))
        assertNotNull(GroupInner.decode(position))
        val sixtieth = position.copyOf().also { it[GroupInner.KEY_ID_BYTES + 1 + 8] = 60 }
        assertNull(GroupInner.decode(sixtieth), "a second of a minute is 0 to 59")
    }

    @Test
    fun `a signature binds the group and the epoch`() {
        val key = MemberKey.generate()
        val groupId = Bytes(ByteArray(16) { 5 })
        val decoded = GroupInner.decode(GroupInner.encode(groupId, 100, key, report))!!
        assertTrue(GroupInner.verify(groupId, 100, decoded, Bytes(key.publicKey)))
        assertTrue(!GroupInner.verify(groupId, 101, decoded, Bytes(key.publicKey)))
        assertTrue(!GroupInner.verify(Bytes(ByteArray(16)), 100, decoded, Bytes(key.publicKey)))
        assertTrue(!GroupInner.verify(groupId, 100, decoded, Bytes(MemberKey.generate().publicKey)))
    }

    private fun GroupBook.groups() = all
}
