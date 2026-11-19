package io.github.throughthewind.echo.adapters.ble

import io.github.throughthewind.echo.core.mesh.Unavailable
import kotlinx.coroutines.flow.Flow

/**
 * The few Bluetooth LE operations the transport needs, so its logic runs on the JVM against a fake
 * and on the phone against [AndroidBleRadio]. Discovery is legacy advertising and scanning; data
 * goes over L2CAP connection-oriented channels, with no GATT server (WI-007, threat model §5.1).
 */
interface BleRadio {
    /** Why the radio cannot be used now, or null if it can. */
    fun unavailable(): Unavailable?

    /**
     * Advertises the app's service with [payload] as its service data, replacing any earlier
     * advertisement. The device name is never included (spec FR-3.4).
     */
    suspend fun advertise(payload: ByteArray)

    suspend fun stopAdvertising()

    /** App phones in range, as they are heard; collecting starts the scan, cancelling stops it. */
    fun sightings(): Flow<Sighting>

    /** Opens an L2CAP server on a fresh PSM. */
    suspend fun listen(): Listener

    /** Opens an L2CAP channel to the phone behind [sighting] on [psm]. Throws on failure. */
    suspend fun connect(
        sighting: Sighting,
        psm: Int,
    ): Link
}

/** One advertisement heard: what it carried, and an opaque way back to the phone that sent it. */
class Sighting(
    val payload: ByteArray,
    /** The platform's device object. Never logged, never stored, never compared across rotations. */
    val target: Any,
)

/** An open L2CAP server. */
interface Listener {
    val psm: Int

    /** Accepts the next incoming channel; throws once the listener is closed. */
    suspend fun accept(): Link

    fun close()
}

/** One open L2CAP channel: a byte stream each way. */
interface Link {
    /** Reads up to [buffer]'s size into it; returns the count, or -1 at the end of the stream. */
    suspend fun read(buffer: ByteArray): Int

    suspend fun write(bytes: ByteArray)

    fun close()
}
