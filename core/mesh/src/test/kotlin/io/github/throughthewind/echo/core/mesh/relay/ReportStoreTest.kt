package io.github.throughthewind.echo.core.mesh.relay

import io.github.throughthewind.echo.core.mesh.PeerHandle
import io.github.throughthewind.echo.core.mesh.report.FrameValidator
import io.github.throughthewind.echo.core.mesh.report.ReportAuthor
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.report.wireMinute
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ReportStoreTest {
    private var now = Instant.parse("2026-11-18T10:00:00Z")
    private val here = GeoPoint(48.8675, 2.3632)
    private val flooder = PeerHandle("flooder")
    private val honest = PeerHandle("honest")

    /** A fresh author each time, so the sender-side rate limit never gets in the way. */
    private fun report(
        type: ReportType = ReportType.TEAR_GAS,
        author: ReportAuthor = ReportAuthor { now },
    ): Pair<ReportAuthor, Validation.Valid> {
        val sent = assertIs<ReportAuthor.Outcome.Sent>(author.report(type, here, hopBudget = 5))
        return author to assertIs<Validation.Valid>(FrameValidator.validate(sent.frame, now.wireMinute()))
    }

    private fun held(
        v: Validation.Valid,
        link: PeerHandle?,
    ) = HeldFrame(v.id, v.frame, v.received, link)

    @Test
    fun `a full store evicts from the busiest link, oldest first`() {
        val store = ReportStore(capacity = 4)
        val (_, mine) = report()
        store.put(held(mine, null))
        val (_, fromHonest) = report()
        store.put(held(fromHonest, honest))
        val flood =
            List(5) {
                now += 1.minutes
                report().second
            }
        val evicted = flood.flatMap { store.put(held(it, flooder)) }
        assertEquals(4, store.size)
        assertTrue(mine.id in store, "this phone's own report is never evicted")
        assertTrue(fromHonest.id in store, "the honest link's report survives the flood")
        assertEquals(flood.take(3).map { it.id }, evicted.map { it.id })
    }

    @Test
    fun `frames are dropped at expiry plus the tolerated skew`() {
        val store = ReportStore(capacity = 10)
        val (_, v) = report(ReportType.EXIT_OPEN) // 15 minutes
        store.put(held(v, honest))
        val created = now.wireMinute()
        assertEquals(0, store.sweep(created + 16))
        assertEquals(1, store.reports(created + 16).size)
        assertEquals(1, store.sweep(created + 17))
        assertFalse(v.id in store)
    }

    @Test
    fun `a withdrawal hides its report, whichever arrives first`() {
        val (author, report) = report()
        val withdrawal = assertIs<Validation.Valid>(FrameValidator.validate(author.withdraw(report.id, 5)!!, now.wireMinute()))

        val reportFirst = ReportStore(capacity = 10)
        reportFirst.put(held(report, honest))
        reportFirst.put(held(withdrawal, honest))
        assertTrue(reportFirst[report.id]!!.withdrawn)
        assertEquals(emptyList(), reportFirst.reports(now.wireMinute()))

        val withdrawalFirst = ReportStore(capacity = 10)
        withdrawalFirst.put(held(withdrawal, honest))
        withdrawalFirst.put(held(report, honest))
        assertTrue(withdrawalFirst[report.id]!!.withdrawn)
        assertEquals(emptyList(), withdrawalFirst.reports(now.wireMinute()))
    }

    @Test
    fun `a withdrawal signed by another key hides nothing`() {
        val (_, report) = report()
        val (other, otherReport) = report(ReportType.MEDIC)
        val store = ReportStore(capacity = 10)
        store.put(held(report, honest))
        // A withdrawal that is valid on its own, but for another report and under another key.
        val foreign = assertIs<Validation.Valid>(FrameValidator.validate(other.withdraw(otherReport.id, 5)!!, now.wireMinute()))
        store.put(held(foreign, flooder))
        assertFalse(store[report.id]!!.withdrawn)
        assertEquals(1, store.reports(now.wireMinute()).size)
    }

    @Test
    fun `token buckets refill at their rate up to their burst`() {
        val bucket = TokenBucket(perSecond = 10.0, burst = 3.0, nowMillis = 0)
        assertEquals(3, (1..10).count { bucket.tryTake(0) })
        assertFalse(bucket.tryTake(50))
        assertTrue(bucket.tryTake(100))
        assertEquals(3, (1..10).count { bucket.tryTake(10_000) })
        // A clock that steps back refills nothing.
        assertFalse(bucket.tryTake(5_000))
    }
}
