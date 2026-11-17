package io.github.throughthewind.echo.adapters.loopback

/**
 * Every frame that reached a phone in a [LoopbackNetwork], in arrival order. Tests read it through
 * [coverage]; nothing in the app sees it.
 */
class Trace {
    /** One frame arriving: when (ms), from which phone, to which phone. */
    data class Delivery(
        val atMillis: Long,
        val from: Int,
        val to: Int,
        val frame: List<Byte>,
    )

    private val lock = Any()
    private val deliveries = mutableListOf<Delivery>()

    internal fun record(
        atMillis: Long,
        from: Int,
        to: Int,
        frame: ByteArray,
    ) = synchronized(lock) { deliveries += Delivery(atMillis, from, to, frame.asList()) }

    fun deliveries(): List<Delivery> = synchronized(lock) { deliveries.toList() }

    /**
     * How the message [key] spread through [phones] phones. A delivery belongs to the message when
     * [keyOf] maps its frame to [key]: the default compares whole frames, a test whose relay changes
     * bytes (a hop budget) passes a function that ignores them.
     */
    fun coverage(
        key: Any,
        phones: Int,
        keyOf: (List<Byte>) -> Any = { it },
    ): Coverage {
        val first = mutableMapOf<Int, Coverage.Receipt>()
        val origins = mutableSetOf<Int>()
        for (d in deliveries()) {
            if (keyOf(d.frame) != key) continue
            // A phone whose frame arrives somewhere before any copy reached it is an origin, at hop 0.
            if (d.from !in first) origins += d.from
            if (d.to in first || d.to in origins) continue
            val senderHop = first[d.from]?.hop ?: 0
            first[d.to] = Coverage.Receipt(d.atMillis, senderHop + 1)
        }
        return Coverage(phones, origins, first)
    }
}

/** How far one message spread: who got it first when, and over how many hops. */
class Coverage(
    val phones: Int,
    /** Phones that sent the message without having received it. */
    val origins: Set<Int>,
    /** Each reached phone's first receipt. Origins are not in it. */
    val receipts: Map<Int, Receipt>,
) {
    data class Receipt(
        val atMillis: Long,
        val hop: Int,
    )

    /** Share of all phones, origins included, holding the message at [millis]. */
    fun fractionAt(millis: Long): Double = (origins.size + receipts.values.count { it.atMillis <= millis }).toDouble() / phones

    /** When the share of phones holding the message first reached [fraction], or null if it never did. */
    fun timeToReach(fraction: Double): Long? {
        val needed = kotlin.math.ceil(fraction * phones).toInt() - origins.size
        if (needed <= 0) return 0
        val times = receipts.values.map { it.atMillis }.sorted()
        return times.getOrNull(needed - 1)
    }

    val maxHop: Int get() = receipts.values.maxOfOrNull { it.hop } ?: 0

    /** The share of phones reached by [millis], every [stepMillis], from 0: the coverage curve. */
    fun curve(
        untilMillis: Long,
        stepMillis: Long,
    ): List<Double> = (0..untilMillis step stepMillis).map(::fractionAt)
}
