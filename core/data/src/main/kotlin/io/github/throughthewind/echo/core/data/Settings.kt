package io.github.throughthewind.echo.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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
)

/** [Settings], sealed in one file of a [SealedFiles], with the current value as a flow. */
class SettingsStore(
    private val files: SealedFiles,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val current = MutableStateFlow(load())

    val settings: StateFlow<Settings> = current.asStateFlow()

    /** Applies [change] and writes the result before returning it. */
    @Synchronized
    fun update(change: (Settings) -> Settings): Settings {
        val next = change(current.value)
        files.write(FILE, json.encodeToString(Settings.serializer(), next).encodeToByteArray())
        current.value = next
        return next
    }

    /** Defaults when there is no file, or when it cannot be opened (the key was destroyed). */
    private fun load(): Settings {
        val bytes = files.read(FILE) ?: return Settings()
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
