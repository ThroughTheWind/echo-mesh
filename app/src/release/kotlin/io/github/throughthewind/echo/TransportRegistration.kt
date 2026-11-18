package io.github.throughthewind.echo

import io.github.throughthewind.echo.core.mesh.MeshTransport

/** Release builds: real radios only. None yet; the first arrives with the BLE adapter (WI-011). */
internal fun registeredTransports(): List<MeshTransport> = emptyList()

internal const val SIMULATED_RADIO = false
