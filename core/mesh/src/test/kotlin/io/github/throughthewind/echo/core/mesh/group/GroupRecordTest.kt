package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.DecodeResult
import io.github.throughthewind.echo.core.mesh.wire.GroupFrame
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.model.ReportType
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

/** A group as it is kept between runs (FR-12.1): everything needed to go on, nothing more. */
class GroupRecordTest {
    private val now = 29_000_000L

    private fun frame(bytes: ByteArray) = assertIs<DecodeResult.Accepted>(WireCodec.decode(bytes)).frame as GroupFrame

    @Test
    fun `a kept group comes back able to read and send for the group`() {
        val book = GroupBook()
        val group = book.create("Crew ü", 24.hours, "Fox", now)!!
        group.add(Member(Bytes(ByteArray(32) { 3 }), "Owl"))
        val record = GroupRecord.encode(group)
        val back = assertNotNull(GroupRecord.decode(record))
        assertEquals(group.id, back.id)
        assertEquals("Crew ü", back.name)
        assertEquals(group.expiresMinute, back.expiresMinute)
        assertEquals(group.roster, back.roster)
        assertEquals(group.self, back.self)
        assertContentEquals(record, GroupRecord.encode(back), "one encoding")

        // The restored member key signs, and the restored epoch key seals, frames the original reads.
        val other = GroupBook().also { it.adopt(back) }
        val sent = other.seal(back, GroupMessage.Report(ReportType.MEDIC, 0, 0, now, 30), now, 7)
        assertEquals("Fox", assertNotNull(book.open(frame(sent))).sender.nickname)
    }

    @Test
    fun `a record holds only the epoch keys held now`() {
        val group = GroupBook().create("Crew", 24.hours, "Fox", now)!!
        group.epochs.advanceTo(GroupBook.epochOf(now) + 10)
        val back = assertNotNull(GroupRecord.decode(GroupRecord.encode(group)))
        assertEquals(group.epochs.oldest, back.epochs.oldest)
        assertEquals(GroupBook.epochOf(now) + 6, back.epochs.oldest, "older keys were deleted before saving")
    }

    @Test
    fun `a record that does not parse exactly gives null, never an exception`() {
        val record = GroupRecord.encode(GroupBook().create("Crew", 24.hours, "Fox", now)!!)
        assertNull(GroupRecord.decode(ByteArray(0)))
        assertNull(GroupRecord.decode(record.copyOf(record.size - 1)))
        assertNull(GroupRecord.decode(record + 0))
        assertNull(GroupRecord.decode(record.copyOf().also { it[0] = 2 }), "unknown version")
        val random = Random(1)
        repeat(2_000) {
            val damaged = record.copyOf().also { it[random.nextInt(it.size)] = random.nextInt(256).toByte() }
            GroupRecord.decode(damaged)
            GroupRecord.decode(random.nextBytes(random.nextInt(400)))
        }
    }

    @Test
    fun `records are named by group id, never by the group's name`() {
        val group = GroupBook().create("Crew", 24.hours, "Fox", now)!!
        val name = GroupRecord.name(group.id)
        assertEquals(GroupRecord.PREFIX.length + 32, name.length)
        assert(name.matches(Regex("group-[0-9a-f]{32}")))
    }
}
