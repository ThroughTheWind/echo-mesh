package io.github.throughthewind.echo.adapters.loopback

import kotlin.math.hypot
import kotlin.random.Random

/** Generated crowd layouts, so a test can ask for 200 phones at street density instead of wiring them. */
object Crowd {
    /**
     * [rows] × [columns] phones on a grid, each linked to its four nearest neighbours: the slowest
     * a report can spread, a hop per row.
     */
    fun grid(
        network: LoopbackNetwork,
        rows: Int,
        columns: Int,
        profile: LinkProfile = LinkProfile.PERFECT,
    ): List<LoopbackTransport> {
        require(rows > 0 && columns > 0) { "a grid needs at least one row and one column" }
        val phones = List(rows * columns) { network.phone() }
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                val here = phones[r * columns + c]
                if (c + 1 < columns) network.link(here, phones[r * columns + c + 1], profile)
                if (r + 1 < rows) network.link(here, phones[(r + 1) * columns + c], profile)
            }
        }
        return phones
    }

    /**
     * [count] phones placed uniformly at random in a square of [side] metres, each linked to every
     * phone within [range] metres: a random geometric graph, the usual model of a crowd with
     * short-range radios. Placement uses [random], so the same seed gives the same crowd.
     */
    fun randomGeometric(
        network: LoopbackNetwork,
        count: Int,
        side: Double,
        range: Double,
        random: Random,
        profile: LinkProfile = LinkProfile.PERFECT,
    ): List<LoopbackTransport> {
        require(count > 0 && side > 0 && range > 0) { "count, side and range must be positive" }
        val phones = List(count) { network.phone() }
        val positions = List(count) { random.nextDouble(side) to random.nextDouble(side) }
        for (i in 0 until count) {
            for (j in i + 1 until count) {
                val (xi, yi) = positions[i]
                val (xj, yj) = positions[j]
                if (hypot(xi - xj, yi - yj) <= range) network.link(phones[i], phones[j], profile)
            }
        }
        return phones
    }
}
