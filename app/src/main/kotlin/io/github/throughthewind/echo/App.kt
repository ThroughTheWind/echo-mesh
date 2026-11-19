package io.github.throughthewind.echo

import android.app.Application
import io.github.throughthewind.echo.core.crypto.Randomness
import io.github.throughthewind.echo.core.crypto.keystore.KeystoreSealingKey
import io.github.throughthewind.echo.core.data.SealedFiles
import io.github.throughthewind.echo.core.data.SettingsStore
import io.github.throughthewind.echo.core.map.MapLibrary
import io.github.throughthewind.echo.core.map.MapSharing
import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.group.SealedRecords
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.mesh.MeshService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * The app-level container (ADR-0001: manual constructor injection). Transports come from
 * TransportRegistration.kt, the only app file allowed to name an adapter (ADR-0002).
 */
class App : Application() {
    /** Lives as long as the process: the mesh and its reports, in memory only (FR-12.2). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val registration: Registration by lazy { registeredTransports(this) }

    val transports: List<MeshTransport> get() = registration.transports

    /** Android permissions the radios need (NFR-10), asked in context on first turn-on. */
    val meshPermissions: List<String> get() = registration.permissions

    /** Everything the app keeps, sealed with a Keystore key a panic wipe destroys (WI-010). */
    private val sealed: SealedFiles by lazy {
        SealedFiles(filesDir.resolve(SEALED_DIRECTORY), KeystoreSealingKey.getOrCreate(SEALING_KEY_ALIAS))
    }

    val settings: SettingsStore by lazy { SettingsStore(sealed) }

    /** Groups and their keys, kept in the same sealed store (FR-12.1); loaded off the main thread by the mesh. */
    private val groupRecords: SealedRecords by lazy {
        object : SealedRecords {
            override fun names() = sealed.names()

            override fun read(name: String) = sealed.read(name)

            override fun write(
                name: String,
                plaintext: ByteArray,
            ) = sealed.write(name, plaintext)

            override fun delete(name: String) = sealed.delete(name)
        }
    }

    val maps: MapLibrary by lazy { MapLibrary(MapSharing.directory(this)) }

    /** One transport in Phase 1; the mesh takes the first registered. */
    val mesh: MeshNode by lazy {
        MeshNode(transports.firstOrNull(), scope, Clock.System::now, Randomness.secure(), groupVault = groupRecords)
    }

    /** True when the only radio is simulated and reaches no other phone. */
    val simulatedRadio: Boolean get() = registration.simulated

    /**
     * Turns the mesh on from a user's tap with the app visible, and with it the foreground service
     * that keeps it relaying in a pocket (FR-3.3). Remembers that it is meant to be on, so a stop
     * the user did not ask for shows as one (FR-3.3a). Returns whether it is on.
     */
    suspend fun meshOn(): Boolean {
        if (!mesh.turnOn()) return false
        withContext(Dispatchers.IO) { settings.update { it.copy(meshMeantOn = true) } }
        if (!simulatedRadio) MeshService.start(this)
        return true
    }

    /** Stops every radio use and the service, and forgets that the mesh was meant to be on (FR-3.1). */
    suspend fun meshOff() {
        MeshService.stop(this)
        mesh.turnOff()
        withContext(Dispatchers.IO) { settings.update { it.copy(meshMeantOn = false) } }
    }

    private companion object {
        const val SEALED_DIRECTORY = "sealed"
        const val SEALING_KEY_ALIAS = "echo-sealing-key"
    }
}
