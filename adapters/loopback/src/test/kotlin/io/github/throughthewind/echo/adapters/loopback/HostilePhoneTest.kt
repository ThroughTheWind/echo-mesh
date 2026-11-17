package io.github.throughthewind.echo.adapters.loopback

import io.github.throughthewind.echo.adapters.loopback.HostilePhone.Attack
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The harness for spec AC-11 and NFR-3: a hostile phone really sends each kind of bad traffic, at
 * 100 times an honest phone's rate, to an honest neighbour. What the mesh does with it is WI-009's.
 */
class HostilePhoneTest {
    private val max = LoopbackNetwork.DEFAULT_CAPABILITIES.maxFrameBytes

    /** An honest phone sends about one frame a second; the attacker sends 100. */
    private val honestRate = 1
    private val attackRate = 100 * honestRate

    @ParameterizedTest
    @EnumSource(Attack::class)
    fun `a hostile phone delivers its attack at 100 times the honest rate`(attack: Attack) =
        runTest {
            val random = Random(11)
            val network = LoopbackNetwork(random = random, scope = backgroundScope, clock = { testScheduler.currentTime })
            val honest = network.phone()
            val other = network.phone()
            val hostile = HostilePhone(network, network.phone(), random)
            network.link(honest, hostile.transport)
            network.link(other, hostile.transport)
            listOf(honest, other, hostile.transport).forEach { it.start() }
            hostile.listen(backgroundScope)
            // Something for the replay attack to replay.
            repeat(5) { other.broadcast(byteArrayOf(42, it.toByte())) }
            testScheduler.runCurrent()

            hostile.attack(this, attack, attackRate, 10.seconds).join()

            val atHonest =
                network.trace
                    .deliveries()
                    .filter { it.to == honest.index && it.from == hostile.transport.index }
                    .map { it.frame }
            assertEquals(10 * attackRate, hostile.sent[attack])
            assertEquals(10 * attackRate, atHonest.size)
            // The attack took ten seconds of virtual time, and none of real time to speak of.
            assertTrue(testScheduler.currentTime >= 10_000 - 10)
            when (attack) {
                Attack.MALFORMED -> {
                    assertTrue(atHonest.all { it.size <= max })
                    assertTrue(atHonest.toSet().size > atHonest.size * 9 / 10, "malformed frames are varied")
                }

                Attack.OVERSIZED -> {
                    assertTrue(atHonest.all { it.size in max + 1..4 * max })
                }

                Attack.REPLAYED -> {
                    val original = List(5) { listOf<Byte>(42, it.toByte()) }.toSet()
                    assertEquals(original, atHonest.toSet())
                }

                Attack.FLOODING -> {
                    assertEquals(atHonest.size, atHonest.toSet().size, "every flooding frame is new")
                }
            }
        }

    @Test
    fun `a custom flood frame is sent as given`() =
        runTest {
            val network = LoopbackNetwork(random = Random(2), scope = backgroundScope)
            val honest = network.phone()
            val hostile = HostilePhone(network, network.phone(), Random(2))
            network.link(honest, hostile.transport)
            honest.start()
            hostile.transport.start()
            hostile.attack(this, Attack.FLOODING, 10, 1.seconds) { n -> byteArrayOf(1, 1, n.toByte()) }.join()
            assertEquals(List(10) { listOf<Byte>(1, 1, it.toByte()) }, network.trace.deliveries().map { it.frame })
        }
}
