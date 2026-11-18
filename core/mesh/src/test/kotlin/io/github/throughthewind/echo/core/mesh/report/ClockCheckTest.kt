package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.mesh.report.ClockCheck.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals

class ClockCheckTest {
    private val now = 29_870_000L

    private fun check(vararg offsets: Long) = ClockCheck().apply { offsets.forEach { observe(now + it, now) } }

    @Test
    fun `too few reports say nothing`() {
        assertEquals(Verdict.UNKNOWN, check(20, 20, 20).verdict(now))
    }

    @Test
    fun `fresh and old reports together are fine`() {
        assertEquals(Verdict.OK, check(0, -1, 0, -2, -12, -30, -45, 0, -1).verdict(now))
    }

    @Test
    fun `most reports from this phone's future mean its clock is behind`() {
        assertEquals(Verdict.BEHIND, check(9, 10, 8, 9, 2, 9, 11, 10).verdict(now))
    }

    @Test
    fun `a few future reports from one hostile peer do not`() {
        assertEquals(Verdict.OK, check(60, 60, 60, 0, -1, 0, -2, -5, 0).verdict(now))
    }

    @Test
    fun `when even the freshest reports are old, this phone's clock is ahead`() {
        assertEquals(Verdict.AHEAD, check(-9, -10, -12, -20, -8, -9, -30, -11).verdict(now))
    }

    @Test
    fun `old observations leave the window`() {
        val check = check(9, 10, 8, 9, 2, 9, 11, 10)
        assertEquals(Verdict.UNKNOWN, check.verdict(now + ClockCheck.WINDOW_MINUTES + 1))
    }
}
