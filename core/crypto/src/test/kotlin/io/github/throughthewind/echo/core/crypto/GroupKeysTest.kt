package io.github.throughthewind.echo.core.crypto

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupKeysTest {
    private val ad = byteArrayOf(1, 4)

    @Test
    fun `a joiner holding one epoch key derives the same later keys as the creator`() {
        val creator = GroupEpochs.create(1000).apply { advanceTo(1002) }
        val joiner = GroupEpochs.fromKey(1002, creator.keyFor(1002)!!)
        for (epoch in 1002L..1004L) assertContentEquals(creator.keyFor(epoch), joiner.keyFor(epoch))
    }

    @Test
    fun `a joiner cannot go back to epochs before it joined`() {
        val creator = GroupEpochs.create(1000)
        val joiner = GroupEpochs.fromKey(1002, creator.keyFor(1002)!!)
        assertNull(joiner.keyFor(1001))
        assertNull(joiner.keyFor(1000))
    }

    @Test
    fun `advancing keeps four epochs back and deletes the rest`() {
        val epochs = GroupEpochs.create(1000)
        epochs.advanceTo(1010)
        assertEquals(1006, epochs.oldest)
        assertEquals(1010, epochs.newest)
        assertNotNull(epochs.keyFor(1006))
        assertNull(epochs.keyFor(1005))
    }

    @Test
    fun `an epoch far ahead is refused rather than derived`() {
        val epochs = GroupEpochs.create(1000)
        assertNotNull(epochs.keyFor(1000L + GroupEpochs.MAX_AHEAD))
        assertNull(epochs.keyFor(1000L + GroupEpochs.MAX_AHEAD + 1))
    }

    @Test
    fun `export and restore give the same keys`() {
        val epochs = GroupEpochs.create(1000).apply { advanceTo(1003) }
        val restored = GroupEpochs.restore(epochs.export())
        for (epoch in 1000L..1005L) assertContentEquals(epochs.keyFor(epoch), restored.keyFor(epoch))
    }

    @Test
    fun `destroying the keys leaves nothing to derive from`() {
        val epochs = GroupEpochs.create(1000)
        val exported = epochs.export()
        epochs.destroy()
        assertTrue(exported.isNotEmpty())
        assertTrue(epochs.export().isEmpty())
    }

    @Test
    fun `a sealed frame opens only under its epoch key and associated data, and every one has one size`() {
        val epochs = GroupEpochs.create(1000)
        val key = epochs.keyFor(1000)!!
        val inner = ByteArray(197) { it.toByte() }
        val sealed = GroupCipher.seal(key, inner, ad)
        assertEquals(237, sealed.size)
        assertContentEquals(inner, GroupCipher.open(key, sealed, ad))
        assertNull(GroupCipher.open(epochs.keyFor(1001)!!, sealed, ad))
        assertNull(GroupCipher.open(GroupEpochs.create(1000).keyFor(1000)!!, sealed, ad))
        assertNull(GroupCipher.open(key, sealed, byteArrayOf(1, 5)))
        for (i in sealed.indices step 17) {
            assertNull(GroupCipher.open(key, sealed.copyOf().also { it[i] = (it[i].toInt() xor 1).toByte() }, ad), "byte $i")
        }
        assertFalse(GroupCipher.seal(key, inner, ad).contentEquals(sealed), "a fresh nonce every time")
    }

    @Test
    fun `a member key signs, verifies and survives export`() {
        val key = MemberKey.generate()
        val message = "report".encodeToByteArray()
        val signature = key.sign(message)
        assertTrue(Signatures.verify(key.publicKey, signature, message))
        val restored = MemberKey.fromSeed(key.exportSeed())
        assertContentEquals(key.publicKey, restored.publicKey)
        assertTrue(Signatures.verify(restored.publicKey, restored.sign(message), message))
    }
}
