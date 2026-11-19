package io.github.throughthewind.echo.core.mesh.group

import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.mesh.wire.Wire
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The join's plaintexts and QR text (ADR-0009): fixed size, one encoding, hostile input refused. */
class JoinPayloadsTest {
    private val fox = Member(Bytes(ByteArray(Wire.PUBLIC_KEY_BYTES) { 7 }), "Fox")
    private val owl = Member(Bytes(ByteArray(Wire.PUBLIC_KEY_BYTES) { 9 }), "Owl ü")

    @Test
    fun `each payload fills its message and reads back`() {
        val request = JoinPayloads.request(fox)
        assertEquals(JoinPayloads.HANDSHAKE_PAYLOAD_BYTES, request.size)
        assertEquals(fox, JoinPayloads.parseRequest(request))

        val welcome = Welcome(Bytes(ByteArray(16) { 1 }), "Crew", 29_000_100, 483_335, ByteArray(32) { 2 }, 47)
        val read = assertNotNull(JoinPayloads.parseWelcome(JoinPayloads.welcome(welcome)))
        assertEquals(welcome.groupId, read.groupId)
        assertEquals("Crew", read.name)
        assertEquals(29_000_100, read.expiresMinute)
        assertEquals(483_335, read.epoch)
        assertEquals(47, read.rosterSize)

        val roster = JoinPayloads.roster(listOf(fox, owl))
        assertEquals(JoinPayloads.TRANSPORT_PAYLOAD_BYTES, roster.size)
        assertEquals(listOf(fox, owl), JoinPayloads.parseRoster(roster, 2))
    }

    @Test
    fun `padding, counts and sizes that are not exactly right are refused`() {
        val request = JoinPayloads.request(fox)
        assertNull(JoinPayloads.parseRequest(request.copyOf(request.size - 1)))
        assertNull(JoinPayloads.parseRequest(request.copyOf().also { it[it.size - 1] = 1 }), "non-zero padding")
        assertNull(JoinPayloads.parseRequest(request.copyOf().also { it[Wire.PUBLIC_KEY_BYTES] = 0 }), "empty nickname")

        val roster = JoinPayloads.roster(listOf(fox, owl))
        assertNull(JoinPayloads.parseRoster(roster, 1), "a second entry where padding should be")
        assertNull(JoinPayloads.parseRoster(roster, 3), "an empty third entry")

        val welcome = JoinPayloads.welcome(Welcome(Bytes(ByteArray(16)), "Crew", 1, 1, ByteArray(32), 1))
        val sizeAt = 16 + 1 + GroupInner.NICKNAME_BYTES + 4 + 4 + 32
        assertNull(JoinPayloads.parseWelcome(welcome.copyOf().also { it[sizeAt] = 0 }), "no roster")
        assertNull(JoinPayloads.parseWelcome(welcome.copyOf().also { it[sizeAt] = Group.MAX_MEMBERS.toByte() }), "no room for the newcomer")
    }

    @Test
    fun `a QR text has one spelling`() {
        val offer = JoinOffer(Bytes(ByteArray(32) { it.toByte() }), Bytes(ByteArray(16) { 0xFF.toByte() }))
        val text = offer.encode()
        assertEquals(JoinOffer.PREFIX.length + 64, text.length)
        assertEquals(offer, JoinOffer.decode(text))
        assertNull(JoinOffer.decode(text.dropLast(1)))
        assertNull(JoinOffer.decode(text.dropLast(1) + "="))
        assertNull(JoinOffer.decode(text.dropLast(1) + "+"), "base64url only")
        assertNull(JoinOffer.decode(text.uppercase()))
        assertNull(JoinOffer.decode("https://example.org/?" + text))
    }
}
