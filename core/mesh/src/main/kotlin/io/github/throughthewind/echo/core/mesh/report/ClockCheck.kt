package io.github.throughthewind.echo.core.mesh.report

/**
 * Tells whether this phone's clock disagrees with the reports it receives (spec FR-7.3, threat
 * model §5.2), so the app can ask the user to fix it. A wrong clock makes a phone drop good reports
 * as expired or from the future, and stamp its own reports wrongly.
 *
 * Peers have no identity, so the only clocks a phone can read are the signed creation times of the
 * reports it receives. Feed it each new message id once, valid or rejected for its time, through
 * [observe]. The thresholds are first guesses that the field test (WI-028) tunes:
 *
 * - **Behind:** honest reports are never created in the receiver's future, so if most recent ones
 *   are more than [THRESHOLD_MINUTES] ahead, this phone's clock is behind.
 * - **Ahead:** a phone receives old reports as well as new ones, so it only says "ahead" when even
 *   its freshest quarter of recent reports is more than [THRESHOLD_MINUTES] old.
 *
 * A hostile peer can make the warning show by flooding mistimed frames; it is a warning, not a
 * change of behaviour, so that costs the user an unneeded check of their clock.
 */
class ClockCheck {
    enum class Verdict { UNKNOWN, OK, BEHIND, AHEAD }

    private class Sample(
        val atMinute: Long,
        val offset: Long,
    )

    private val lock = Any()
    private val samples = ArrayDeque<Sample>()

    /** One message id first seen at [nowMinute], whose signed creation minute is [createdMinute]. */
    fun observe(
        createdMinute: Long,
        nowMinute: Long,
    ) = synchronized(lock) {
        samples.addLast(Sample(nowMinute, createdMinute - nowMinute))
        while (samples.size > MAX_SAMPLES) samples.removeFirst()
    }

    fun verdict(nowMinute: Long): Verdict =
        synchronized(lock) {
            val recent = samples.filter { nowMinute - it.atMinute in 0..WINDOW_MINUTES }.map { it.offset }.sorted()
            if (recent.size < MIN_SAMPLES) return Verdict.UNKNOWN
            val median = recent[recent.size / 2]
            val freshestQuarter = recent[(recent.size * 3) / 4]
            when {
                median > THRESHOLD_MINUTES -> Verdict.BEHIND
                freshestQuarter < -THRESHOLD_MINUTES -> Verdict.AHEAD
                else -> Verdict.OK
            }
        }

    companion object {
        const val THRESHOLD_MINUTES: Long = 5
        const val WINDOW_MINUTES: Long = 10
        const val MIN_SAMPLES: Int = 8
        const val MAX_SAMPLES: Int = 256
    }
}
