package io.github.throughthewind.echo

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.BatteryManager
import io.github.throughthewind.echo.core.crypto.Randomness
import io.github.throughthewind.echo.core.crypto.keystore.KeystoreSealingKey
import io.github.throughthewind.echo.core.data.SealedFiles
import io.github.throughthewind.echo.core.data.SettingsStore
import io.github.throughthewind.echo.core.map.MapLibrary
import io.github.throughthewind.echo.core.map.MapSharing
import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.diagnostics.FieldDiagnostics
import io.github.throughthewind.echo.core.mesh.group.SealedRecords
import io.github.throughthewind.echo.core.mesh.node.MeshNode
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.mesh.DiagnosticsProvider
import io.github.throughthewind.echo.mesh.DiscreetTile
import io.github.throughthewind.echo.mesh.MeshService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

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

    private var batteryReader: Job? = null

    override fun onCreate() {
        super.onCreate()
        // Diagnostics left on count again from this launch. The mesh still meant on at launch
        // means the process was killed with it running (FR-3.3a, FR-17.1).
        scope.launch(Dispatchers.IO) {
            val kept = settings.settings.value
            if (kept.diagnostics) startDiagnostics(killedBefore = kept.meshMeantOn)
        }
    }

    /**
     * Turns field diagnostics on or off (FR-17.1, FR-17.4). Off forgets every count at once; only
     * the switch is kept. The counts are never written anywhere until the user shares them.
     */
    suspend fun setDiagnostics(on: Boolean) {
        withContext(Dispatchers.IO) { settings.update { it.copy(diagnostics = on) } }
        if (on) {
            startDiagnostics(killedBefore = false)
        } else {
            batteryReader?.cancel()
            batteryReader = null
            mesh.stopDiagnostics()
            DiagnosticsProvider.forget()
        }
    }

    private fun startDiagnostics(killedBefore: Boolean) {
        mesh.startDiagnostics(killedBefore)
        batteryReader?.cancel()
        batteryReader =
            scope.launch {
                val battery = getSystemService(BatteryManager::class.java)
                while (isActive) {
                    battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }?.let(mesh::batteryLevel)
                    delay(FieldDiagnostics.BATTERY_EVERY)
                }
            }
    }

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

    /**
     * Starts sharing with a group, from the user's tap with Echo visible, then starts the service
     * again so it adds the `location` type while that is allowed (WI-075, NFR-10).
     */
    fun startSharing(
        id: Bytes,
        until: Instant?,
    ): Boolean {
        if (!mesh.startSharing(id, until)) return false
        if (!simulatedRadio) MeshService.start(this)
        return true
    }

    /** Stops every radio use and the service, and forgets that the mesh was meant to be on (FR-3.1). */
    suspend fun meshOff() {
        // Forgotten first, so the service stopping here is not counted as killed (FR-17.1).
        withContext(Dispatchers.IO) { settings.update { it.copy(meshMeantOn = false) } }
        MeshService.stop(this)
        mesh.turnOff()
    }

    /**
     * Turns discreet mode on or off (FR-13.1), from the map's eye button or the Quick Settings tile.
     * The tile and the mesh notification follow at once.
     */
    suspend fun setDiscreet(on: Boolean) {
        withContext(Dispatchers.IO) { settings.update { it.copy(discreet = on) } }
        DiscreetTile.refresh(this)
    }

    /**
     * The panic wipe (FR-11.2, threat model §5.6): the sealing key is destroyed first, so everything
     * kept is unreadable at once; then the files go, the mesh stops without a word on the air
     * (FR-11.4), and group keys in memory are destroyed. Maps go too when [includeMaps].
     * [restartFresh] then starts a new process, so nothing in memory survives either (FR-11.3).
     */
    suspend fun wipe(includeMaps: Boolean) {
        withContext(Dispatchers.IO) { sealed.destroy() }
        batteryReader?.cancel()
        DiagnosticsProvider.forget()
        MeshService.stop(this)
        mesh.wipe()
        withContext(Dispatchers.IO) {
            val mapsDirectory = MapSharing.directory(this@App)
            if (includeMaps) maps.deleteAll()
            filesDir
                .listFiles()
                .orEmpty()
                .filter { includeMaps || it != mapsDirectory }
                .forEach { it.deleteRecursively() }
            cacheDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
            noBackupFilesDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
        }
    }

    /** Opens the app again as a fresh install would, in a new process: onboarding first (FR-11.3). */
    fun restartFresh(activity: Activity) {
        activity.startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        activity.finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    private companion object {
        const val SEALED_DIRECTORY = "sealed"
        const val SEALING_KEY_ALIAS = "echo-sealing-key"
    }
}
