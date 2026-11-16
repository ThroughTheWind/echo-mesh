package io.github.throughthewind.echo

import android.app.Application
import io.github.throughthewind.echo.core.mesh.MeshTransport

/**
 * The app-level container (ADR-0001: manual constructor injection). Transports come from
 * TransportRegistration.kt, the only app file allowed to name an adapter (ADR-0002).
 */
class App : Application() {
    val transports: List<MeshTransport> by lazy { registeredTransports() }
}
