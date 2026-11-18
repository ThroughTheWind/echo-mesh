package io.github.throughthewind.echo

import android.app.Application
import io.github.throughthewind.echo.core.crypto.Randomness
import io.github.throughthewind.echo.core.crypto.keystore.KeystoreSealingKey
import io.github.throughthewind.echo.core.data.SealedFiles
import io.github.throughthewind.echo.core.data.SettingsStore
import io.github.throughthewind.echo.core.map.MapLibrary
import io.github.throughthewind.echo.core.map.MapSharing
import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock

/**
 * The app-level container (ADR-0001: manual constructor injection). Transports come from
 * TransportRegistration.kt, the only app file allowed to name an adapter (ADR-0002).
 */
class App : Application() {
    /** Lives as long as the process: the mesh and its reports, in memory only (FR-12.2). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val transports: List<MeshTransport> by lazy { registeredTransports() }

    /** Settings, sealed with a Keystore key a panic wipe destroys (WI-010). */
    val settings: SettingsStore by lazy {
        SettingsStore(SealedFiles(filesDir.resolve(SEALED_DIRECTORY), KeystoreSealingKey.getOrCreate(SEALING_KEY_ALIAS)))
    }

    val maps: MapLibrary by lazy { MapLibrary(MapSharing.directory(this)) }

    /** One transport in Phase 1; the mesh takes the first registered. */
    val mesh: MeshNode by lazy { MeshNode(transports.firstOrNull(), scope, Clock.System::now, Randomness.secure()) }

    /** True in development builds, whose only radio is simulated and reaches no other phone. */
    val simulatedRadio: Boolean get() = SIMULATED_RADIO

    private companion object {
        const val SEALED_DIRECTORY = "sealed"
        const val SEALING_KEY_ALIAS = "echo-sealing-key"
    }
}
