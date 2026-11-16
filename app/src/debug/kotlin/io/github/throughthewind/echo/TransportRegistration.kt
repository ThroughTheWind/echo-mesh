package io.github.throughthewind.echo

import io.github.throughthewind.echo.adapters.loopback.LoopbackNetwork
import io.github.throughthewind.echo.core.mesh.MeshTransport

/** Debug builds: a simulated radio only, until the first real transport lands (ADR-0002). */
internal fun registeredTransports(): List<MeshTransport> = listOf(LoopbackNetwork().phone())
