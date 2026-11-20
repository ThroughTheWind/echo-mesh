package io.github.throughthewind.echo.core.data

import io.github.throughthewind.echo.core.crypto.Sealers
import io.github.throughthewind.echo.core.crypto.SealingKey
import io.github.throughthewind.echo.core.crypto.SealingUnavailableException
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SealedFilesTest {
    @TempDir
    lateinit var dir: File

    private fun containsPlaintext(
        file: File,
        text: String,
    ) = file.readBytes().decodeToString().contains(text)

    @Test
    fun `settings survive a restart, and nothing on disk is plaintext`() {
        val key = Sealers.ephemeral()
        val first = SettingsStore(SealedFiles(dir, key))
        first.update { it.copy(onboardingDone = true, lastAudienceIsPublic = false) }

        val again = SettingsStore(SealedFiles(dir, key))
        assertEquals(Settings(onboardingDone = true, lastAudienceIsPublic = false), again.settings.value)
        val files = dir.listFiles()!!.toList()
        assertEquals(listOf("settings"), files.map { it.name })
        assertFalse(files.any { containsPlaintext(it, "onboardingDone") || containsPlaintext(it, "true") })
    }

    @Test
    fun `display and feedback choices survive a restart, and an older settings file still opens`() {
        val key = Sealers.ephemeral()
        val files = SealedFiles(dir, key)
        SettingsStore(files).update { it.copy(theme = "Sun", largePins = true, haptics = "Strong") }
        val again = SettingsStore(SealedFiles(dir, key)).settings.value
        assertEquals(Triple("Sun", true, "Strong"), Triple(again.theme, again.largePins, again.haptics))

        // A file written before these fields existed opens with their defaults.
        files.write("settings", """{"onboardingDone":true,"discreet":true}""".encodeToByteArray())
        val old = SettingsStore(files).settings.value
        assertEquals(Settings(onboardingDone = true, discreet = true), old)
    }

    @Test
    fun `destroying the key makes every file unreadable, then deletes them`() {
        val key = Sealers.ephemeral()
        val files = SealedFiles(dir, key)
        files.write("settings", "secret".encodeToByteArray())
        files.write("group-1", "group key".encodeToByteArray())
        val copies = dir.listFiles()!!.associate { it.name to it.readBytes() }

        files.destroy()
        assertTrue(dir.listFiles()!!.isEmpty())
        // A copy of the files taken before the wipe cannot be opened either: the key is gone.
        for ((name, bytes) in copies) File(dir, name).writeBytes(bytes)
        assertNull(files.read("settings"))
        assertNull(files.read("group-1"))
        assertEquals(Settings(), SettingsStore(files).settings.value)
        assertFailsWith<IllegalStateException> { files.write("settings", byteArrayOf(1)) }
    }

    @Test
    fun `a file swapped for another, or changed, does not open`() {
        val files = SealedFiles(dir, Sealers.ephemeral())
        files.write("a", "first".encodeToByteArray())
        files.write("b", "second".encodeToByteArray())
        File(dir, "a").writeBytes(File(dir, "b").readBytes())
        assertNull(files.read("a"))
        val b = File(dir, "b")
        b.writeBytes(b.readBytes().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() })
        assertNull(files.read("b"))
    }

    @Test
    fun `another key cannot read the files`() {
        SealedFiles(dir, Sealers.ephemeral()).write("settings", "x".encodeToByteArray())
        assertNull(SealedFiles(dir, Sealers.ephemeral()).read("settings"))
    }

    @Test
    fun `deleting a file removes it, and names stay inside the directory`() {
        val files = SealedFiles(dir, Sealers.ephemeral())
        files.write("group-7", byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), files.read("group-7"))
        files.delete("group-7")
        assertEquals(emptySet(), files.names())
        assertFailsWith<IllegalArgumentException> { files.write("../escape", byteArrayOf(1)) }
        assertFailsWith<IllegalArgumentException> { files.read("Settings") }
    }

    /** A key that refuses every use while [locked], as an Android Keystore key does on a locked phone. */
    private class LockableKey(
        private val key: SealingKey = Sealers.ephemeral(),
    ) : SealingKey by key {
        var locked = false

        override fun seal(
            plaintext: ByteArray,
            context: ByteArray,
        ): ByteArray =
            if (locked) throw SealingUnavailableException(IllegalStateException("Device locked")) else key.seal(plaintext, context)

        override fun open(
            sealed: ByteArray,
            context: ByteArray,
        ): ByteArray? = if (locked) throw SealingUnavailableException(IllegalStateException("Device locked")) else key.open(sealed, context)
    }

    @Test
    fun `a setting changed while the phone is locked applies at once and is written after unlock`() {
        val key = LockableKey()
        val store = SettingsStore(SealedFiles(dir, key))
        store.update { it.copy(onboardingDone = true) }
        key.locked = true
        // The discreet tile works on the lock screen: no crash, and the change shows at once.
        assertTrue(store.update { it.copy(discreet = true) }.discreet)
        assertTrue(store.settings.value.discreet)
        assertFalse(SettingsStore(SealedFiles(dir, key.also { it.locked = false })).settings.value.discreet)

        key.locked = true
        store.flush()
        key.locked = false
        store.flush()
        assertEquals(Settings(onboardingDone = true, discreet = true), SettingsStore(SealedFiles(dir, key)).settings.value)
    }

    @Test
    fun `settings first opened while locked are never written over with defaults`() {
        val key = LockableKey()
        SettingsStore(SealedFiles(dir, key)).update { it.copy(onboardingDone = true, haptics = "Strong") }
        key.locked = true
        val store = SettingsStore(SealedFiles(dir, key))
        store.update { it.copy(discreet = true) }
        assertTrue(store.settings.value.discreet)

        key.locked = false
        store.flush()
        // The change made while locked is replayed on the real settings, which were kept.
        val expected = Settings(onboardingDone = true, haptics = "Strong", discreet = true)
        assertEquals(expected, store.settings.value)
        assertEquals(expected, SettingsStore(SealedFiles(dir, key)).settings.value)
    }

    @Test
    fun `a locked phone reads as unavailable, not as a missing file`() {
        val key = LockableKey()
        val files = SealedFiles(dir, key)
        files.write("group-1", byteArrayOf(1))
        key.locked = true
        assertFailsWith<SealingUnavailableException> { files.read("group-1") }
        assertFailsWith<SealingUnavailableException> { files.write("group-1", byteArrayOf(2)) }
        key.locked = false
        assertContentEquals(byteArrayOf(1), files.read("group-1"))
    }
}
