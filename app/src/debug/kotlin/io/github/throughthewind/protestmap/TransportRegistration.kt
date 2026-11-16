package io.github.throughthewind.protestmap

import io.github.throughthewind.protestmap.adapters.loopback.LoopbackNetwork
import io.github.throughthewind.protestmap.core.mesh.MeshTransport

/** Debug builds: a simulated radio only, until the first real transport lands (ADR-0002). */
internal fun registeredTransports(): List<MeshTransport> = listOf(LoopbackNetwork().phone())
