package io.github.throughthewind.echo.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * What the app remembers between runs, Phase 1. Nothing here may say where the user has been: no
 * position, no report, no peer (spec FR-12.2). Fields are added by the items that need them.
 */
@Serializable
data class Settings(
    /** The user finished onboarding (FR-1). */
    val onboardingDone: Boolean = false,
    /** The last audience picked on the send button: everyone nearby, or a group (FR-4.3). */
    val lastAudienceIsPublic: Boolean = true,
    /** The group last sent to, by its random id in hex, when [lastAudienceIsPublic] is false. Never its name. */
    val lastAudienceGroup: String? = null,
    /**
     * The user turned the mesh on and has not turned it off. Found true with the mesh off at launch,
     * relaying stopped without the user's say (FR-3.3a). Never turns the mesh back on by itself.
     */
    val meshMeantOn: Boolean = false,
    /** Android's Nearby devices dialog was shown once; after that the fix is in Settings (FR-1.2). */
    val nearbyAsked: Boolean = false,
    /** Android's Notifications dialog was shown once (FR-1.2). */
    val notificationsAsked: Boolean = false,
    /** Discreet mode (FR-13.1): darkest theme, dimmed, silent, a neutral notification. Not a disguise (FR-13.2). */
    val discreet: Boolean = false,
    /**
     * Field diagnostics are on (FR-17.1). Only the switch is kept: the counts live in memory and
     * start again at each launch (FR-17.4).
     */
    val diagnostics: Boolean = false,
    /** Settings > Display > Theme: `Auto`, `Dark` or `Sun`, the design system's `ThemePref` by name. */
    val theme: String = "Auto",
    /** Settings > Display > Large pins. Discreet mode overrides it. */
    val largePins: Boolean = false,
    /** Settings > Feedback > Haptics: `Off`, `Light` or `Strong`, the design system's `HapticsLevel` by name. */
    val haptics: String = "Light",
)

/** [Settings], sealed in one file of a [SealedFiles], with the current value as a flow. */
class SettingsStore(
    private val files: SealedFiles,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // While the phone is locked the file can be neither read nor written (WI-077). Changes made then
    // (the discreet tile works on the lock screen) apply at once in memory, and are kept to be
    // replayed on the file's real contents by [flush] once it opens: never written over a file that
    // could not be read, never lost.
    private var unopened = false
    private val replay = mutableListOf<(Settings) -> Settings>()
    private var unwritten = false
    private val current = MutableStateFlow(load())

    val settings: StateFlow<Settings> = current.asStateFlow()

    /**
     * Applies [change] and writes the result before returning it. While the phone is locked the
     * result applies in memory and is written by [flush], or by the next update after unlock.
     */
    @Synchronized
    fun update(change: (Settings) -> Settings): Settings {
        if (unopened) replay += change else unwritten = true
        current.value = change(current.value)
        flush()
        return current.value
    }

    /** Writes what the phone was locked for, if anything. Called when the phone is unlocked. */
    @Synchronized
    fun flush() {
        if (unopened) {
            val kept = loadOrNull() ?: return
            unopened = false
            current.value = replay.fold(kept) { settings, change -> change(settings) }
            replay.clear()
            unwritten = true
        }
        if (!unwritten) return
        try {
            files.write(FILE, json.encodeToString(Settings.serializer(), current.value).encodeToByteArray())
            unwritten = false
        } catch (_: IOException) {
            // Locked, or the disk refused: kept in memory, written by the next flush.
        }
    }

    private fun load(): Settings = loadOrNull() ?: Settings().also { unopened = true }

    /**
     * The saved settings: defaults when there is no file, or when it cannot be opened (the key was
     * destroyed); null while the phone is locked.
     */
    private fun loadOrNull(): Settings? {
        val bytes =
            try {
                files.read(FILE)
            } catch (_: IOException) {
                return null
            } ?: return Settings()
        return try {
            json.decodeFromString(Settings.serializer(), bytes.decodeToString())
        } catch (_: SerializationException) {
            Settings()
        } catch (_: IllegalArgumentException) {
            Settings()
        }
    }

    private companion object {
        const val FILE = "settings"
    }
}
