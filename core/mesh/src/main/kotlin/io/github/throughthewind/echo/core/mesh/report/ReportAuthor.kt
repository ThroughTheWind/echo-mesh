package io.github.throughthewind.echo.core.mesh.report

import io.github.throughthewind.echo.core.crypto.Hashes
import io.github.throughthewind.echo.core.crypto.OneTimeSigningKey
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.PublicReportFrame
import io.github.throughthewind.echo.core.mesh.wire.PublicVoteFrame
import io.github.throughthewind.echo.core.mesh.wire.SignedFrame
import io.github.throughthewind.echo.core.mesh.wire.Vote
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.mesh.wire.WireCodec
import io.github.throughthewind.echo.core.mesh.wire.WithdrawalFrame
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.Report
import io.github.throughthewind.echo.core.model.ReportType
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Makes this phone's public reports and withdrawals (spec FR-4.4 to FR-4.6, threat model §5.2).
 *
 * Every report is signed with a fresh [OneTimeSigningKey], so two reports from one phone share
 * nothing but what they say. The key stays in memory until the report expires, to sign a
 * withdrawal, and is never stored. Times are cut to the minute and positions to the wire's grid
 * (about 11 m) before signing, so nothing finer leaves the phone.
 */
class ReportAuthor(
    private val clock: () -> Instant,
) {
    sealed interface Outcome {
        /** A frame ready for the mesh. [report] is what it says, as the receivers will see it. */
        class Sent(
            val id: Bytes,
            val frame: ByteArray,
            val report: Report,
        ) : Outcome

        /** The same type was reported less than [RATE_LIMIT] ago (FR-4.6); nothing was signed. */
        data object RateLimited : Outcome
    }

    private class Held(
        val key: OneTimeSigningKey,
        val expiresMinute: Long,
    )

    private val lock = Any()
    private val held = mutableMapOf<Bytes, Held>()
    private val lastSent = mutableMapOf<ReportType, Instant>()

    /**
     * Signs a report of [type] at [position], to travel [hopBudget] hops. [lifetime] is a whole
     * number of minutes, 1 to [Wire.MAX_LIFETIME_MINUTES].
     */
    fun report(
        type: ReportType,
        position: GeoPoint,
        hopBudget: Int,
        lifetime: Duration = type.defaultLifetime,
    ): Outcome {
        val minutes = lifetime.inWholeMinutes
        require(
            lifetime == minutes.minutes && minutes in 1..Wire.MAX_LIFETIME_MINUTES,
        ) { "lifetime must be 1 to 180 whole minutes: $lifetime" }
        require(hopBudget in 0..Wire.MAX_HOP_BUDGET) { "hop budget out of range: $hopBudget" }
        val now = clock()
        synchronized(lock) {
            forgetExpired(now.wireMinute())
            val last = lastSent[type]
            if (last != null && now < last + RATE_LIMIT) return Outcome.RateLimited
            val key = OneTimeSigningKey.generate()
            val unsigned =
                PublicReportFrame(
                    hopBudget = hopBudget,
                    publicKey = Bytes(key.publicKey),
                    type = type,
                    latitude = WireUnits.of(position.latitude),
                    longitude = WireUnits.of(position.longitude),
                    createdMinute = now.wireMinute(),
                    lifetimeMinutes = minutes.toInt(),
                    signature = Bytes(ByteArray(Wire.SIGNATURE_BYTES)),
                )
            val signed = unsigned.copy(signature = Bytes(key.sign(WireCodec.signingInput(unsigned))))
            val id = idOf(signed)
            held[id] = Held(key, signed.createdMinute + signed.lifetimeMinutes)
            lastSent[type] = now
            return Outcome.Sent(id, WireCodec.encode(signed), signed.toReport())
        }
    }

    /**
     * Signs a withdrawal of this phone's report [id] with the report's own key, or returns null if
     * this phone holds no live key for it (not its report, already expired, or the app restarted).
     * The withdrawal lives until the report would have expired.
     */
    fun withdraw(
        id: Bytes,
        hopBudget: Int,
    ): ByteArray? {
        require(hopBudget in 0..Wire.MAX_HOP_BUDGET) { "hop budget out of range: $hopBudget" }
        val nowMinute = clock().wireMinute()
        synchronized(lock) {
            forgetExpired(nowMinute)
            val report = held.remove(id) ?: return null
            val unsigned =
                WithdrawalFrame(
                    hopBudget = hopBudget,
                    publicKey = Bytes(report.key.publicKey),
                    target = id,
                    createdMinute = nowMinute,
                    lifetimeMinutes = (report.expiresMinute - nowMinute).toInt().coerceIn(1, Wire.MAX_LIFETIME_MINUTES),
                    signature = Bytes(ByteArray(Wire.SIGNATURE_BYTES)),
                )
            return WireCodec.encode(unsigned.copy(signature = Bytes(report.key.sign(WireCodec.signingInput(unsigned)))))
        }
    }

    /**
     * Counts a report of [type] that is not signed here (a group report) against the same rate limit
     * as public ones (FR-4.6). False, and nothing counted, when [type] is rate-limited now.
     */
    fun takeSlot(type: ReportType): Boolean {
        val now = clock()
        synchronized(lock) {
            val last = lastSent[type]
            if (last != null && now < last + RATE_LIMIT) return false
            lastSent[type] = now
            return true
        }
    }

    /**
     * Signs a public [vote] on the report [target], with a fresh one-time key so no two votes from
     * this phone can be linked (FR-6.1, FR-6.2). It lives until [untilMinute], the latest the report
     * could live, at most [Wire.MAX_LIFETIME_MINUTES]. One vote per report is the caller's rule.
     */
    fun vote(
        target: Bytes,
        vote: Vote,
        untilMinute: Long,
        hopBudget: Int,
    ): ByteArray {
        require(target.size == Wire.MESSAGE_ID_BYTES) { "a message id is 32 bytes" }
        require(hopBudget in 0..Wire.MAX_HOP_BUDGET) { "hop budget out of range: $hopBudget" }
        val nowMinute = clock().wireMinute()
        val key = OneTimeSigningKey.generate()
        val unsigned =
            PublicVoteFrame(
                hopBudget = hopBudget,
                publicKey = Bytes(key.publicKey),
                target = target,
                vote = vote,
                createdMinute = nowMinute,
                lifetimeMinutes = (untilMinute - nowMinute).toInt().coerceIn(1, Wire.MAX_LIFETIME_MINUTES),
                signature = Bytes(ByteArray(Wire.SIGNATURE_BYTES)),
            )
        return WireCodec.encode(unsigned.copy(signature = Bytes(key.sign(WireCodec.signingInput(unsigned)))))
    }

    /** When [type] may be reported again (FR-4.6), or null if it may be now. */
    fun nextAllowed(type: ReportType): Instant? =
        synchronized(lock) {
            lastSent[type]?.plus(RATE_LIMIT)?.takeIf { it > clock() }
        }

    /** Ids of this phone's reports that it can still withdraw. */
    fun withdrawable(): Set<Bytes> =
        synchronized(lock) {
            forgetExpired(clock().wireMinute())
            held.keys.toSet()
        }

    private fun forgetExpired(nowMinute: Long) {
        held.entries.removeAll { nowMinute >= it.value.expiresMinute }
    }

    companion object {
        /** At most one report per type per minute from this phone (FR-4.6): against mistakes only. */
        val RATE_LIMIT: Duration = 1.minutes

        /** The message id of a frame this phone made: SHA-256 of it without the hop budget (FR-5.2). */
        internal fun idOf(frame: SignedFrame): Bytes {
            val bytes = WireCodec.encode(frame)
            val content = bytes.copyOfRange(0, Wire.HOP_BUDGET_OFFSET) + bytes.copyOfRange(Wire.HOP_BUDGET_OFFSET + 1, bytes.size)
            return Bytes(Hashes.sha256(content))
        }
    }
}

/** Positions on the air are whole units of 1/[Wire.DEGREE_UNITS] degree (ADR-0005, FR-4.4). */
object WireUnits {
    fun of(degrees: Double): Int = (degrees * Wire.DEGREE_UNITS).roundToInt()

    fun degrees(units: Int): Double = units.toDouble() / Wire.DEGREE_UNITS
}

/** Minutes since the Unix epoch, UTC: the wire's clock (FR-7.3). */
fun Instant.wireMinute(): Long = Math.floorDiv(epochSeconds, 60L)

fun instantOfWireMinute(minute: Long): Instant = Instant.fromEpochSeconds(minute * 60)

/** What a valid report frame says, for the map. */
fun PublicReportFrame.toReport(): Report =
    Report(
        type = type,
        position = GeoPoint(WireUnits.degrees(latitude), WireUnits.degrees(longitude)),
        createdAt = instantOfWireMinute(createdMinute),
        lifetime = lifetimeMinutes.minutes,
    )
