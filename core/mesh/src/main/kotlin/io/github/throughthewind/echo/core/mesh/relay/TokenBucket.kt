package io.github.throughthewind.echo.core.mesh.relay

/**
 * A rate limit: [perSecond] tokens a second, at most [burst] saved up. Time is in milliseconds from
 * any origin; a clock that steps back refills nothing.
 */
internal class TokenBucket(
    private val perSecond: Double,
    private val burst: Double,
    nowMillis: Long,
) {
    private var tokens = burst
    private var lastMillis = nowMillis

    fun tryTake(
        nowMillis: Long,
        cost: Double = 1.0,
    ): Boolean {
        if (nowMillis > lastMillis) {
            tokens = minOf(burst, tokens + (nowMillis - lastMillis) * perSecond / 1_000.0)
            lastMillis = nowMillis
        }
        if (tokens < cost) return false
        tokens -= cost
        return true
    }
}
