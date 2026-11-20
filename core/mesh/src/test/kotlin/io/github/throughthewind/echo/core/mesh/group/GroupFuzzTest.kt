package io.github.throughthewind.echo.core.mesh.group

import com.code_intelligence.jazzer.junit.FuzzTest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * The group decoders' fuzz target (.ai/rules/crypto.md): the inner message a member's key let
 * through, the join payloads, and the text of a scanned QR code. Seeds and findings live under
 * `src/test/resources/…/GroupFuzzTestInputs/decode/`; CI fuzzes it on every pull request, like
 * [io.github.throughthewind.echo.core.mesh.wire.WireFuzzTest]. The first byte picks the decoder.
 */
class GroupFuzzTest {
    @FuzzTest(maxDuration = "1m")
    fun decode(data: ByteArray) = GroupProperty.check(data)
}

/** Every decoder takes any bytes without throwing, and what it accepts has exactly one encoding. */
object GroupProperty {
    fun check(data: ByteArray) {
        if (data.isEmpty()) return
        val input = data.copyOfRange(1, data.size)
        when (data[0].toInt() and 0x07) {
            0 -> {
                val decoded = GroupInner.decode(input) ?: return
                assertEquals(GroupInner.SIZE, input.size)
                assertContentEquals(input.copyOf(GroupInner.SIGNED_BYTES), decoded.body)
                assertContentEquals(input.copyOfRange(GroupInner.SIGNED_BYTES, input.size), decoded.signature)
            }

            1 -> {
                val member = JoinPayloads.parseRequest(input) ?: return
                assertContentEquals(input, JoinPayloads.request(member))
            }

            2 -> {
                val welcome = JoinPayloads.parseWelcome(input) ?: return
                assertContentEquals(input, JoinPayloads.welcome(welcome))
            }

            3 -> {
                if (input.isEmpty()) return
                val members = JoinPayloads.parseRoster(input.copyOfRange(1, input.size), input[0].toInt()) ?: return
                assertContentEquals(input.copyOfRange(1, input.size), JoinPayloads.roster(members))
            }

            else -> {
                val text = input.decodeToString()
                val offer = JoinOffer.decode(text) ?: return
                assertEquals(text, offer.encode())
            }
        }
    }
}
