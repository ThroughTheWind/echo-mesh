package io.github.throughthewind.echo.core.data

import io.github.throughthewind.echo.core.crypto.SealingKey
import java.io.File
import java.io.IOException

/**
 * Small encrypted files in the app's private storage (spec FR-12.1, threat model §5.6): settings,
 * and in Phase 2 groups and their keys. Reports, votes and positions never come here (FR-12.2).
 *
 * Each file is sealed by [key] with its own name as context, so files cannot be swapped. Writes go
 * to a temporary file first and replace the old one in one rename, so a crash leaves the old
 * contents or the new ones, never half of each.
 */
class SealedFiles(
    private val directory: File,
    private val key: SealingKey,
) {
    init {
        directory.mkdirs()
        require(directory.isDirectory) { "not a directory: $directory" }
    }

    fun write(
        name: String,
        plaintext: ByteArray,
    ) {
        val target = fileFor(name)
        val temporary = File(directory, "$name$TEMPORARY")
        temporary.writeBytes(key.seal(plaintext, context(name)))
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IOException("could not replace $name")
        }
    }

    /** The contents of [name], or null when it does not exist or cannot be opened (key destroyed, tampered). */
    fun read(name: String): ByteArray? {
        val file = fileFor(name)
        if (!file.isFile) return null
        return key.open(file.readBytes(), context(name))
    }

    /** Deletes [name]: expired data is deleted, not hidden (FR-12.4). */
    fun delete(name: String) {
        fileFor(name).delete()
    }

    fun names(): Set<String> =
        directory
            .listFiles()
            .orEmpty()
            .filter { it.isFile && !it.name.endsWith(TEMPORARY) }
            .map { it.name }
            .toSet()

    /**
     * Destroys the key first, so every file is unreadable at once (crypto-erase), then deletes the
     * files. The order is the panic wipe's (threat model §5.6, WI-024).
     */
    fun destroy() {
        key.destroy()
        directory.listFiles().orEmpty().forEach { it.delete() }
    }

    private fun fileFor(name: String): File {
        require(NAME.matches(name)) { "file names are lower-case letters, digits and dashes: $name" }
        return File(directory, name)
    }

    private fun context(name: String) = "echo/sealed/$name".encodeToByteArray()

    private companion object {
        const val TEMPORARY = ".tmp"
        val NAME = Regex("[a-z0-9-]{1,64}")
    }
}
