package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.adapters.loopback.HostilePhone.Attack
import io.github.throughthewind.echo.core.mesh.relay.Relay
import io.github.throughthewind.echo.core.mesh.relay.RelayConfig
import io.github.throughthewind.echo.core.mesh.report.ReportAuthor
import io.github.throughthewind.echo.core.mesh.report.Validation
import io.github.throughthewind.echo.core.mesh.wire.Wire
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.core.model.ReportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The relay (WI-009) on the loopback simulator: spec AC-6, AC-9 and AC-11. */
class RelaySimulationTest {
    private val here = GeoPoint(48.8675, 2.3632)
    private val epoch = Instant.parse("2026-11-18T10:00:00Z")

    private class Mesh(
        val network: LoopbackNetwork,
        val phones: List<LoopbackTransport>,
        val relays: List<Relay>,
        val clock: () -> Instant,
    )

    /** Starts a relay on every phone, under virtual time, then the phones themselves. */
    private suspend fun TestScope.mesh(
        random: Random,
        config: RelayConfig = RelayConfig(),
        build: (LoopbackNetwork) -> List<LoopbackTransport>,
    ): Mesh {
        val start = testScheduler.currentTime
        val network = LoopbackNetwork(random = random, scope = backgroundScope, clock = { testScheduler.currentTime - start })
        val clock = { epoch + (testScheduler.currentTime - start).milliseconds }
        val phones = build(network)
        val relays = phones.map { Relay(it, clock, random, config).also { relay -> relay.run(backgroundScope) } }
        phones.forEach { it.start() }
        return Mesh(network, phones, relays, clock)
    }

    /** The message [id]'s spread, ignoring the hop budget byte that relays change. */
    private fun Mesh.coverage(frame: ByteArray): Coverage {
        val key = frame.asList().filterIndexed { i, _ -> i != Wire.HOP_BUDGET_OFFSET }
        return network.trace.coverage(key, phones.size) { f -> f.filterIndexed { i, _ -> i != Wire.HOP_BUDGET_OFFSET } }
    }

    private fun sent(outcome: ReportAuthor.Outcome) = assertIs<ReportAuthor.Outcome.Sent>(outcome)

    @Test
    fun `a report reaches a neighbour, then disappears from both at expiry and is never sent again`() =
        runTest {
            val mesh = mesh(Random(1)) { net -> List(2) { net.phone() }.also { (a, b) -> net.link(a, b) } }
            val author = ReportAuthor(mesh.clock)
            val report = sent(author.report(ReportType.EXIT_OPEN, here, hopBudget = 5)) // 15 minutes
            assertIs<Validation.Valid>(mesh.relays[0].publish(report.frame))
            delay(1.seconds)
            assertEquals(
                listOf(report.id),
                mesh.relays[1]
                    .reports.value
                    .map { it.id },
            )
            assertEquals(
                listOf(report.id),
                mesh.relays[0]
                    .reports.value
                    .map { it.id },
            )

            // AC-6: gone from both once its signed expiry and the tolerated skew have passed.
            delay(17.minutes)
            mesh.relays.forEach { it.sweep() }
            mesh.relays.forEach { assertEquals(emptyList(), it.reports.value) }
            mesh.relays.forEach { assertTrue(!it.holds(report.id)) }
            val lastSeen =
                mesh.network.trace
                    .deliveries()
                    .filter { it.frame.size == report.frame.size }
                    .maxOf { it.atMillis }
            delay(5.minutes)
            val after =
                mesh.network.trace
                    .deliveries()
                    .filter { it.frame.size == report.frame.size && it.atMillis > lastSeen }
            assertEquals(emptyList(), after, "an expired report was sent again")
        }

    @Test
    fun `a phone that arrives late gets what it missed from a summary`() =
        runTest {
            val mesh = mesh(Random(2)) { net -> List(2) { net.phone() } }
            val author = ReportAuthor(mesh.clock)
            val report = sent(author.report(ReportType.MEDIC, here, hopBudget = 5))
            mesh.relays[0].publish(report.frame)
            delay(1.minutes)
            assertEquals(emptyList(), mesh.relays[1].reports.value, "not linked yet")
            // They come into range: each sends a summary on meeting, and the gap is pushed back.
            mesh.network.link(mesh.phones[0], mesh.phones[1])
            delay(1.seconds)
            assertEquals(
                listOf(report.id),
                mesh.relays[1]
                    .reports.value
                    .map { it.id },
            )
        }

    @Test
    fun `a withdrawal spreads and hides the report everywhere`() =
        runTest {
            val mesh = mesh(Random(3)) { net -> Crowd.grid(net, 3, 3) }
            val author = ReportAuthor(mesh.clock)
            val report = sent(author.report(ReportType.POLICE_LINE, here, hopBudget = 7))
            mesh.relays[0].publish(report.frame)
            delay(5.seconds)
            assertTrue(mesh.relays.all { r -> r.reports.value.any { it.id == report.id } })
            mesh.relays[0].publish(assertNotNull(author.withdraw(report.id, hopBudget = 7)))
            delay(5.seconds)
            assertTrue(mesh.relays.all { it.reports.value.isEmpty() })
        }

    @Test
    fun `the hop budget caps how far a report travels`() =
        runTest {
            // A line of ten phones. The author sends with a budget of 3, each relay takes one off, and
            // the phone that receives 0 keeps it without passing it on: four links in all.
            val mesh = mesh(Random(4), RelayConfig(summaryEvery = 1.minutes)) { net -> Crowd.grid(net, 10, 1) }
            val author = ReportAuthor(mesh.clock)
            val report = sent(author.report(ReportType.KETTLE, here, hopBudget = 3))
            mesh.relays[0].publish(report.frame)
            delay(3.minutes)
            val reached =
                mesh.relays.indices.filter { i ->
                    mesh.relays[i]
                        .reports.value
                        .any { it.id == report.id }
                }
            assertEquals((0..4).toList(), reached)
        }

    @Test
    fun `AC-9 - a report crosses 5 hops and reaches 90 percent of a 200-phone crowd within 60 s`() =
        runTest {
            val random = Random(7)
            val mesh =
                mesh(random) { net ->
                    Crowd.randomGeometric(net, 200, side = 100.0, range = 15.0, random = random, profile = LinkProfile.CROWDED_BLE)
                }
            delay(2.seconds) // Phones meet and swap empty summaries.
            val author = ReportAuthor(mesh.clock)
            val report = sent(author.report(ReportType.TEAR_GAS, here, hopBudget = RelayConfig.DEFAULT_HOP_BUDGET))
            val sentAt = testScheduler.currentTime
            mesh.relays[0].publish(report.frame)
            delay(60.seconds)
            val coverage = mesh.coverage(report.frame)
            val time = assertNotNull(coverage.timeToReach(0.9)) - sentAt
            assertTrue(time < 60_000, "90% took $time ms")
            assertTrue(coverage.maxHop >= 5, "crossed only ${coverage.maxHop} hops")
        }

    @ParameterizedTest
    @EnumSource(Attack::class)
    fun `AC-11 - a peer attacking at 100 times the honest rate does not stop an honest report`(attack: Attack) =
        runTest(timeout = 5.minutes) {
            val random = Random(11)
            lateinit var hostile: HostilePhone
            val mesh =
                mesh(random) { net ->
                    val crowd =
                        Crowd.randomGeometric(
                            net,
                            60,
                            side = 60.0,
                            range = 15.0,
                            random = random,
                            profile = LinkProfile.CROWDED_BLE,
                        )
                    hostile = HostilePhone(net, net.phone(), random)
                    // The attacker sits in the middle of the crowd, in range of a dozen phones.
                    crowd.take(12).forEach { net.link(it, hostile.transport, LinkProfile.CROWDED_BLE) }
                    crowd
                }
            hostile.transport.start()
            hostile.listen(backgroundScope)
            // The flood is the worst case: validly signed fakes, each costing a signature check.
            val flood: ((Int) -> ByteArray)? = if (attack == Attack.FLOODING) ({ n -> fakeReport(mesh.clock, n) }) else null
            delay(2.seconds)
            val honestAuthor = ReportAuthor(mesh.clock)
            val warmup = sent(honestAuthor.report(ReportType.MEDIC, here, hopBudget = 7))
            mesh.relays[30].publish(warmup.frame) // Something to replay.
            delay(5.seconds)
            hostile.attack(backgroundScope, attack, framesPerSecond = 100, duration = 10.seconds, floodFrame = flood)
            delay(5.seconds)
            val report = sent(honestAuthor.report(ReportType.TEAR_GAS, here, hopBudget = 7))
            val sentAt = testScheduler.currentTime
            mesh.relays[59].publish(report.frame)
            delay(60.seconds)
            val coverage = mesh.coverage(report.frame)
            val honest = mesh.phones.size
            // Honest phones only: the attacker's own receipt does not count.
            val times =
                coverage.receipts
                    .filterKeys { it < honest }
                    .values
                    .map { it.atMillis }
                    .sorted()
            val needed = kotlin.math.ceil(0.9 * honest).toInt() - 1
            assertTrue(times.size >= needed, "$attack: reached ${times.size + 1} of $honest")
            val time = times[needed - 1] - sentAt
            assertTrue(time < 60_000, "$attack: 90% took $time ms")
            assertTrue(hostile.sent.getValue(attack) > 0)
        }

    /** A fake report signed with its own fresh key: valid, distinct, and worthless. */
    private fun fakeReport(
        clock: () -> Instant,
        n: Int,
    ): ByteArray {
        val type = ReportType.entries[n % ReportType.entries.size]
        val position = GeoPoint(48.0 + n * 1e-4, 2.0)
        // A fresh author per frame: a modified app ignores the sender-side rate limit (FR-4.6).
        return sent(ReportAuthor(clock).report(type, position, hopBudget = 7)).frame
    }
}
