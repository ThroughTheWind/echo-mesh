package io.github.throughthewind.echo.core.mesh.diagnostics

import io.github.throughthewind.echo.core.mesh.LinkFailure
import io.github.throughthewind.echo.core.mesh.PeerHandle
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Field diagnostics (spec F17, FR-17.1 to FR-17.4, WI-036): counts of how the mesh behaved, kept in
 * memory from [start] until the user turns them off or exports them. Nothing here is ever written
 * to disk, and nothing is ever sent: the user exports [export] by hand.
 *
 * The fields are a closed set, the ones FR-17.1 lists, and nothing else: no position, no report
 * content or type, no map area, no group name, no nickname, no key and no token (FR-17.2). Peer
 * handles are held only for the current minute, to count them, and only the count is kept. Every
 * count is stamped in minutes since [start], never with a date or a time of day (threat model §10).
 */
class FieldDiagnostics(
    private val start: Instant,
) {
    /** One minute of the session. [peers] distinct peer handles; [received] new reports from other phones; [relayed] frames passed on. */
    data class Minute(
        val minute: Int,
        val peers: Int,
        val received: Int,
        val relayed: Int,
        val failures: Map<LinkFailure, Int>,
    )

    /** Everything an export holds: the FR-17.1 fields, by minute since diagnostics were turned on. */
    data class Snapshot(
        /** Minutes since diagnostics were turned on, up to now. */
        val length: Int,
        val minutes: List<Minute>,
        /** Delivery delay of public reports, in whole minutes from creation to arrival, and how many arrived with it. */
        val delays: Map<Int, Int>,
        /** Battery percent, read every [BATTERY_EVERY], by minute. */
        val battery: List<Pair<Int, Int>>,
        /** Minutes at which the mesh stopped without the user turning it off (the foreground service was killed). */
        val serviceKilled: List<Int>,
        /** The app started with the mesh meant on: the process was killed before this session (FR-3.3a). */
        val killedBefore: Boolean,
    )

    private val minutes = sortedMapOf<Int, MutableMinute>()
    private val delays = sortedMapOf<Int, Int>()
    private val battery = mutableListOf<Pair<Int, Int>>()
    private val killed = mutableListOf<Int>()
    private var killedBefore = false

    // The current minute's peer handles, to count them. Never kept past the minute, never exported.
    private var peerMinute = -1
    private val peersThisMinute = HashSet<PeerHandle>()

    private class MutableMinute {
        var peers = 0
        var received = 0
        var relayed = 0
        val failures = sortedMapOf<LinkFailure, Int>()
    }

    private fun minuteOf(now: Instant): Int = ((now - start).inWholeMinutes).toInt().coerceAtLeast(0)

    private fun at(now: Instant): MutableMinute = minutes.getOrPut(minuteOf(now)) { MutableMinute() }

    /** A peer was heard from at [now]: counted once per minute however often it is heard. */
    @Synchronized
    fun peer(
        peer: PeerHandle,
        now: Instant,
    ) {
        val minute = minuteOf(now)
        if (minute != peerMinute) {
            peersThisMinute.clear()
            peerMinute = minute
        }
        if (peersThisMinute.add(peer)) at(now).peers++
    }

    /** A new report arrived from another phone at [now]; [createdAt] when it was made, if it can be read. */
    @Synchronized
    fun received(
        now: Instant,
        createdAt: Instant?,
    ) {
        at(now).received++
        if (createdAt != null) {
            val delay = (now - createdAt).inWholeMinutes.toInt().coerceAtLeast(0)
            delays[delay] = (delays[delay] ?: 0) + 1
        }
    }

    /** [count] frames were passed on to other phones at [now]. */
    @Synchronized
    fun relayed(
        now: Instant,
        count: Int,
    ) {
        if (count > 0) at(now).relayed += count
    }

    /** A link to a nearby phone failed at [now], for the reason [kind]. */
    @Synchronized
    fun failed(
        kind: LinkFailure,
        now: Instant,
    ) {
        val m = at(now)
        m.failures[kind] = (m.failures[kind] ?: 0) + 1
    }

    /** The battery level at [now], in percent. Kept at most once per [BATTERY_EVERY]. */
    @Synchronized
    fun battery(
        percent: Int,
        now: Instant,
    ) {
        val minute = minuteOf(now)
        val last = battery.lastOrNull()?.first
        if (last != null && minute - last < BATTERY_EVERY.inWholeMinutes) return
        battery += minute to percent.coerceIn(0, 100)
    }

    /** The mesh stopped at [now] without the user turning it off: the foreground service was killed. */
    @Synchronized
    fun serviceKilled(now: Instant) {
        killed += minuteOf(now)
    }

    /** The app started with the mesh still meant on, so the process was killed before this session. */
    @Synchronized
    fun killedBeforeStart() {
        killedBefore = true
    }

    /** A copy of everything counted up to [now]. */
    @Synchronized
    fun snapshot(now: Instant): Snapshot =
        Snapshot(
            length = minuteOf(now),
            minutes = minutes.map { (minute, m) -> Minute(minute, m.peers, m.received, m.relayed, m.failures.toMap()) },
            delays = delays.toMap(),
            battery = battery.toList(),
            serviceKilled = killed.toList(),
            killedBefore = killedBefore,
        )

    /** The export file, plain text, exactly as the user sees it before sharing it (FR-17.3). */
    fun export(now: Instant): String = DiagnosticsFile.write(snapshot(now))

    companion object {
        /** How often the battery level is kept (FR-17.1). */
        val BATTERY_EVERY = 10.minutes
    }
}

/**
 * The export's format: plain text in a few CSV tables, so a field tester can read every line before
 * sharing it and the team can load it without Echo. The headers are the closed set of FR-17.1; a
 * test (NFR-19) holds them to it.
 */
object DiagnosticsFile {
    const val TITLE = "Echo field diagnostics, format 1"
    const val NOTE =
        "Minutes count from when diagnostics were turned on. No date, no time of day, no position, " +
            "no report, no group, no name, no key and no token is in this file."

    val MINUTE_HEADER: List<String> =
        listOf("minute", "peers", "reports_received", "frames_relayed") + LinkFailure.entries.map { "failed_${it.name.lowercase()}" }
    val DELAY_HEADER: List<String> = listOf("delay_minutes", "reports")
    val BATTERY_HEADER: List<String> = listOf("minute", "battery_percent")
    val KILLED_HEADER: List<String> = listOf("minute", "service_killed")
    const val LENGTH = "session_minutes"
    const val KILLED_BEFORE = "killed_before_session"

    /** Every header and key the file may hold: the FR-17.1 fields. */
    val FIELDS: Set<String> =
        (MINUTE_HEADER + DELAY_HEADER + BATTERY_HEADER + KILLED_HEADER + LENGTH + KILLED_BEFORE).toSet()

    fun write(s: FieldDiagnostics.Snapshot): String =
        buildString {
            appendLine(TITLE)
            appendLine(NOTE)
            appendLine()
            appendLine("$LENGTH,${s.length}")
            appendLine("$KILLED_BEFORE,${if (s.killedBefore) "yes" else "no"}")
            appendLine()
            appendLine(MINUTE_HEADER.joinToString(","))
            for (m in s.minutes) {
                val failures = LinkFailure.entries.map { m.failures[it] ?: 0 }
                appendLine((listOf(m.minute, m.peers, m.received, m.relayed) + failures).joinToString(","))
            }
            appendLine()
            appendLine(DELAY_HEADER.joinToString(","))
            for ((delay, count) in s.delays) appendLine("$delay,$count")
            appendLine()
            appendLine(BATTERY_HEADER.joinToString(","))
            for ((minute, percent) in s.battery) appendLine("$minute,$percent")
            appendLine()
            appendLine(KILLED_HEADER.joinToString(","))
            for (minute in s.serviceKilled) appendLine("$minute,yes")
        }
}
