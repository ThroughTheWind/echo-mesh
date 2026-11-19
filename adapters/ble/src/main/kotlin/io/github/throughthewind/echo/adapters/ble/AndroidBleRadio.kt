package io.github.throughthewind.echo.adapters.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import io.github.throughthewind.echo.core.mesh.Unavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * [BleRadio] on Android's Bluetooth stack (minSdk 31, so no location permission: `BLUETOOTH_SCAN`
 * carries `neverForLocation`). Legacy advertising, filtered scanning, insecure L2CAP channels; no
 * pairing, no GATT server, never the device name (spec FR-3.4, threat model §5.1).
 */
@SuppressLint("MissingPermission") // Every call is behind unavailable(), which checks the permissions.
class AndroidBleRadio(
    context: Context,
) : BleRadio {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = manager?.adapter
    private var advertising: AdvertiseCallback? = null

    override fun unavailable(): Unavailable? {
        val adapter = adapter ?: return Unavailable.UNSUPPORTED
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return Unavailable.UNSUPPORTED
        if (PERMISSIONS.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            return Unavailable.PERMISSION_MISSING
        }
        if (!adapter.isEnabled) return Unavailable.RADIO_OFF
        return null
    }

    override suspend fun advertise(payload: ByteArray) {
        stopAdvertising()
        val advertiser = adapter?.bluetoothLeAdvertiser ?: throw IOException("no advertiser")
        val settings =
            AdvertiseSettings
                .Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(0)
                .build()
        // Legacy advertising holds 31 bytes: the 128-bit service UUID in the advertisement, the
        // service data (UUID, token, PSM: 24 bytes) in the scan response. Never the device name.
        val data =
            AdvertiseData
                .Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(SERVICE)
                .build()
        val response =
            AdvertiseData
                .Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(SERVICE, payload)
                .build()
        val callback = object : AdvertiseCallback() {}
        advertising = callback
        advertiser.startAdvertising(settings, data, response, callback)
    }

    override suspend fun stopAdvertising() {
        val callback = advertising ?: return
        advertising = null
        runCatching { adapter?.bluetoothLeAdvertiser?.stopAdvertising(callback) }
    }

    override fun sightings(): Flow<Sighting> =
        callbackFlow {
            val scanner = adapter?.bluetoothLeScanner ?: throw IOException("no scanner")
            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult,
                    ) {
                        sighting(result)?.let { trySend(it) }
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>) {
                        results.forEach { result -> sighting(result)?.let { trySend(it) } }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        close(IOException("scan failed: $errorCode"))
                    }
                }
            val filter = ScanFilter.Builder().setServiceUuid(SERVICE).build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
            scanner.startScan(listOf(filter), settings, callback)
            awaitClose { runCatching { scanner.stopScan(callback) } }
        }

    private fun sighting(result: ScanResult): Sighting? {
        val data = result.scanRecord?.getServiceData(SERVICE) ?: return null
        return Sighting(data, result.device)
    }

    override suspend fun listen(): Listener {
        val server =
            adapter?.listenUsingInsecureL2capChannel() ?: throw IOException("no Bluetooth adapter")
        return AndroidListener(server)
    }

    override suspend fun connect(
        sighting: Sighting,
        psm: Int,
    ): Link {
        val device = sighting.target as? BluetoothDevice ?: throw IOException("not a Bluetooth device")
        return withContext(Dispatchers.IO) {
            val socket = device.createInsecureL2capChannel(psm)
            try {
                socket.connect()
            } catch (e: IOException) {
                runCatching { socket.close() }
                throw e
            }
            AndroidLink(socket)
        }
    }

    private class AndroidListener(
        private val server: BluetoothServerSocket,
    ) : Listener {
        override val psm: Int = server.psm

        override suspend fun accept(): Link = withContext(Dispatchers.IO) { AndroidLink(server.accept()) }

        override fun close() {
            runCatching { server.close() }
        }
    }

    private class AndroidLink(
        private val socket: BluetoothSocket,
    ) : Link {
        override suspend fun read(buffer: ByteArray): Int = withContext(Dispatchers.IO) { socket.inputStream.read(buffer) }

        override suspend fun write(bytes: ByteArray) =
            withContext(Dispatchers.IO) {
                socket.outputStream.write(bytes)
                socket.outputStream.flush()
            }

        override fun close() {
            runCatching { socket.close() }
        }
    }

    companion object {
        private val SERVICE = ParcelUuid(Advertisement.SERVICE_UUID)

        /** The "Nearby devices" group (spec NFR-10). Location is never asked for. */
        val PERMISSIONS =
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
    }
}
