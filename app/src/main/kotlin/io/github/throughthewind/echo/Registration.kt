package io.github.throughthewind.echo

import io.github.throughthewind.echo.core.mesh.MeshTransport

/** What TransportRegistration.kt gives the app, without naming a radio (ADR-0002). */
internal class Registration(
    val transports: List<MeshTransport>,
    /** The only radio is simulated and reaches no other phone: the mesh sheet says so. */
    val simulated: Boolean,
    /** Android permissions the radios need, asked in context when the mesh is first turned on (FR-1.2). */
    val permissions: List<String>,
)
