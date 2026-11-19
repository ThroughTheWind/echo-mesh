package io.github.throughthewind.echo

import android.content.Context
import io.github.throughthewind.echo.adapters.ble.AndroidBleRadio
import io.github.throughthewind.echo.adapters.ble.BleTransport
import io.github.throughthewind.echo.adapters.loopback.LoopbackNetwork
import io.github.throughthewind.echo.core.crypto.Randomness
import io.github.throughthewind.echo.core.mesh.Unavailable

/**
 * Debug builds: the real Bluetooth LE radio, as in release, so two development phones reach each
 * other. A phone or emulator with no Bluetooth LE gets a simulated radio instead, and the mesh sheet
 * says so (ADR-0002).
 */
internal fun registeredTransports(context: Context): Registration {
    val radio = AndroidBleRadio(context)
    return if (radio.unavailable() == Unavailable.UNSUPPORTED) {
        Registration(listOf(LoopbackNetwork().phone()), simulated = true, permissions = emptyList())
    } else {
        Registration(listOf(BleTransport(radio, Randomness.secure())), simulated = false, permissions = AndroidBleRadio.PERMISSIONS)
    }
}
