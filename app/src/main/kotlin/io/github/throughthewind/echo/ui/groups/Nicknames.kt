package io.github.throughthewind.echo.ui.groups

import kotlin.random.Random

/**
 * Random friendly nicknames ("Yellow Otter", FR-8.4): never the phone owner's name or contacts. Each
 * fits the 32 UTF-8 bytes a nickname may take on the air (ADR-0009).
 */
object Nicknames {
    private val colours =
        listOf("Yellow", "Blue", "Green", "Red", "Orange", "Purple", "Silver", "Golden", "Teal", "Coral", "Amber", "Indigo")
    private val animals =
        listOf("Otter", "Fox", "Owl", "Heron", "Badger", "Lynx", "Wren", "Hare", "Seal", "Crane", "Moth", "Finch", "Newt", "Marten")

    fun random(random: Random = Random.Default): String = "${colours.random(random)} ${animals.random(random)}"

    /** The most UTF-8 bytes a nickname or group name may take (ADR-0009). */
    const val MAX_BYTES = 32

    fun fits(text: String): Boolean = text.isNotBlank() && text.trim().encodeToByteArray().size <= MAX_BYTES
}
