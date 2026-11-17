package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.core.mesh.TransportEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A phone running a modified app (threat model A4): it sends what an honest phone never would, at a
 * rate an honest phone never would. It is the harness for spec NFR-3 and AC-11: mesh tests put one
 * next to honest phones and check they keep working.
 */
class HostilePhone(
    private val network: LoopbackNetwork,
    val transport: LoopbackTransport,
    private val random: Random,
    private val maxFrameBytes: Int = LoopbackNetwork.DEFAULT_CAPABILITIES.maxFrameBytes,
) {
    enum class Attack {
        /** Random bytes of random length within the size limit. */
        MALFORMED,

        /** Frames longer than the radio's limit, up to four times it, as a modified stack can send. */
        OVERSIZED,

        /** Frames it received from others, sent again unchanged. */
        REPLAYED,

        /** A stream of distinct frames, each of them [floodFrame] if given, otherwise random. */
        FLOODING,
    }

    private val heard = mutableListOf<ByteArray>()

    /** Frames sent so far, per attack, so a test can tell the harness did what it claims. */
    val sent: Map<Attack, Int> get() = synchronized(counts) { counts.toMap() }
    private val counts = mutableMapOf<Attack, Int>()

    /** Remembers what it hears, for [Attack.REPLAYED]. */
    fun listen(scope: CoroutineScope): Job =
        scope.launch {
            transport.events.filterIsInstance<TransportEvent.FrameReceived>().collect { event ->
                synchronized(heard) { if (heard.size < MAX_HEARD) heard += event.frame }
            }
        }

    /**
     * Sends [attack] frames to every neighbour at [framesPerSecond] for [duration], in [scope] (under
     * virtual time in tests). [floodFrame] builds the n-th flooding frame, for floods that must look
     * valid up to their signature.
     */
    fun attack(
        scope: CoroutineScope,
        attack: Attack,
        framesPerSecond: Int,
        duration: Duration,
        floodFrame: ((Int) -> ByteArray)? = null,
    ): Job {
        require(framesPerSecond > 0) { "a rate must be positive" }
        val gap = 1.seconds / framesPerSecond
        val total = (duration / gap).toInt()
        return scope.launch {
            for (n in 0 until total) {
                if (!isActive) break
                val frame = frame(attack, n, floodFrame)
                if (frame != null) {
                    network.deliver(transport, null, frame)
                    synchronized(counts) { counts[attack] = (counts[attack] ?: 0) + 1 }
                }
                delay(gap)
            }
        }
    }

    private fun frame(
        attack: Attack,
        n: Int,
        floodFrame: ((Int) -> ByteArray)?,
    ): ByteArray? =
        when (attack) {
            Attack.MALFORMED -> {
                random.nextBytes(random.nextInt(0, maxFrameBytes + 1))
            }

            Attack.OVERSIZED -> {
                random.nextBytes(random.nextInt(maxFrameBytes + 1, 4 * maxFrameBytes + 1))
            }

            Attack.REPLAYED -> {
                synchronized(heard) { if (heard.isEmpty()) null else heard[n % heard.size].copyOf() }
            }

            Attack.FLOODING -> {
                floodFrame?.invoke(n) ?: random.nextBytes(maxFrameBytes)
            }
        }

    private companion object {
        const val MAX_HEARD = 1_000
    }
}
