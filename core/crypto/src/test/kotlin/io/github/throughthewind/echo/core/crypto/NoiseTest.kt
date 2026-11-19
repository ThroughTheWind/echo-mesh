package io.github.throughthewind.echo.core.crypto

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The join handshake against the published cacophony vectors (ADR-0004), then against an attacker. */
class NoiseTest {
    private class Vector(
        val fields: Map<String, ByteArray>,
        val messages: List<Pair<ByteArray, ByteArray>>,
    )

    private fun hex(s: String): ByteArray = if (s == "-") ByteArray(0) else s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private val vectors: List<Vector> =
        run {
            val text = requireNotNull(javaClass.getResource("/noise/NKpsk0.txt")).readText()
            val out = mutableListOf<Vector>()
            var fields = mutableMapOf<String, ByteArray>()
            var messages = mutableListOf<Pair<ByteArray, ByteArray>>()
            for (line in text.lines().filter { it.isNotBlank() && !it.startsWith("#") }) {
                val parts = line.split(" ")
                when (parts[0]) {
                    "vector" -> {
                        if (fields.isNotEmpty()) out += Vector(fields, messages)
                        fields = mutableMapOf()
                        messages = mutableListOf()
                    }

                    "message" -> {
                        messages += hex(parts[1]) to hex(parts[2])
                    }

                    else -> {
                        fields[parts[0]] = hex(parts[1])
                    }
                }
            }
            out += Vector(fields, messages)
            out
        }

    @Test
    fun `the handshake and transport messages match the cacophony vectors byte for byte`() {
        assertTrue(vectors.isNotEmpty())
        for (v in vectors) {
            val f = v.fields
            val initiator =
                NoiseHandshake(true, f.getValue("init_prologue"), f.getValue("psk"), null, f.getValue("init_remote_static"), f.getValue("init_ephemeral"))
            val responder =
                NoiseHandshake(false, f.getValue("init_prologue"), f.getValue("psk"), f.getValue("resp_static"), null, f.getValue("resp_ephemeral"))

            val (p1, c1) = v.messages[0]
            assertContentEquals(c1, initiator.writeMessage(p1))
            assertContentEquals(p1, responder.readMessage(c1))
            val (p2, c2) = v.messages[1]
            assertContentEquals(c2, responder.writeMessage(p2))
            assertContentEquals(p2, initiator.readMessage(c2))

            assertContentEquals(f.getValue("handshake_hash"), initiator.handshakeHash)
            assertContentEquals(f.getValue("handshake_hash"), responder.handshakeHash)

            val i = initiator.split()
            val r = responder.split()
            v.messages.drop(2).forEachIndexed { index, (payload, ciphertext) ->
                val (from, to) = if (index % 2 == 0) i to r else r to i
                assertContentEquals(ciphertext, from.encrypt(payload))
                assertContentEquals(payload, to.decrypt(ciphertext))
            }
        }
    }

    private val psk = ByteArray(32) { it.toByte() }
    private val prologue = "echo-join".encodeToByteArray()

    @Test
    fun `two fresh sides agree`() {
        val static = NoiseStaticKey.generate()
        val joiner = NoiseHandshake.initiator(static.publicKey, psk, prologue)
        val member = NoiseHandshake.responder(static, psk, prologue)
        assertContentEquals("hello".encodeToByteArray(), member.readMessage(joiner.writeMessage("hello".encodeToByteArray())))
        assertContentEquals("secret".encodeToByteArray(), joiner.readMessage(member.writeMessage("secret".encodeToByteArray())))
        assertTrue(joiner.isComplete && member.isComplete)
        assertContentEquals(joiner.handshakeHash, member.handshakeHash)
    }

    @Test
    fun `a wrong PSK, a wrong key or a changed byte is refused, and the handshake dies`() {
        val static = NoiseStaticKey.generate()
        val first = NoiseHandshake.initiator(static.publicKey, psk, prologue).writeMessage(ByteArray(0))

        assertNull(NoiseHandshake.responder(static, ByteArray(32), prologue).readMessage(first))
        assertNull(NoiseHandshake.responder(NoiseStaticKey.generate(), psk, prologue).readMessage(first))
        assertNull(NoiseHandshake.responder(static, psk, "other".encodeToByteArray()).readMessage(first))
        for (i in first.indices) {
            val changed = first.copyOf().also { it[i] = (it[i].toInt() xor 1).toByte() }
            assertNull(NoiseHandshake.responder(static, psk, prologue).readMessage(changed), "byte $i")
        }
        val dead = NoiseHandshake.responder(static, psk, prologue)
        assertNull(dead.readMessage(first.copyOf(first.size - 1)))
        assertNull(dead.readMessage(first), "a handshake that failed once stays dead")
    }

    @Test
    fun `a replayed reply does not open a second handshake`() {
        val static = NoiseStaticKey.generate()
        val joiner = NoiseHandshake.initiator(static.publicKey, psk, prologue)
        val member = NoiseHandshake.responder(static, psk, prologue)
        assertNotNull(member.readMessage(joiner.writeMessage(ByteArray(0))))
        val reply = member.writeMessage("group".encodeToByteArray())

        val other = NoiseHandshake.initiator(static.publicKey, psk, prologue)
        other.writeMessage(ByteArray(0))
        assertNull(other.readMessage(reply), "a reply belongs to the joiner whose ephemeral key it answers")
        assertNotNull(joiner.readMessage(reply))
    }

    @Test
    fun `transport messages refuse replay and reordering`() {
        val static = NoiseStaticKey.generate()
        val joiner = NoiseHandshake.initiator(static.publicKey, psk, prologue)
        val member = NoiseHandshake.responder(static, psk, prologue)
        member.readMessage(joiner.writeMessage(ByteArray(0)))
        joiner.readMessage(member.writeMessage(ByteArray(0)))
        val a = joiner.split()
        val b = member.split()
        val one = a.encrypt(byteArrayOf(1))
        val two = a.encrypt(byteArrayOf(2))
        assertNull(b.decrypt(two))
        assertContentEquals(byteArrayOf(1), b.decrypt(one))
        assertNull(b.decrypt(one))
        assertEquals(2, b.decrypt(two)!![0].toInt())
    }
}
