package io.github.throughthewind.echo.adapters.ble

import io.github.throughthewind.echo.core.mesh.MeshTransport
import io.github.throughthewind.echo.core.mesh.MeshTransportContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.random.Random

/** The BLE transport's logic meets the contract every transport meets (.ai/rules/transports.md). */
class BleTransportContractTest : MeshTransportContract() {
    override fun CoroutineScope.inRange(): Pair<MeshTransport, MeshTransport> {
        val air = FakeAir()
        return BleTransport(air.radio(), Random(1), context = coroutineContext.minusKey(Job)) to
            BleTransport(air.radio(), Random(2), context = coroutineContext.minusKey(Job))
    }
}
