package io.github.throughthewind.echo

import android.content.Context
import io.github.throughthewind.echo.adapters.ble.AndroidBleRadio
import io.github.throughthewind.echo.adapters.ble.BleTransport
import io.github.throughthewind.echo.core.crypto.Randomness

/** Release builds: real radios only. Bluetooth LE first (WI-011); a phone without it is told so. */
internal fun registeredTransports(context: Context): Registration =
    Registration(
        transports = listOf(BleTransport(AndroidBleRadio(context), Randomness.secure())),
        simulated = false,
        permissions = AndroidBleRadio.PERMISSIONS,
    )
