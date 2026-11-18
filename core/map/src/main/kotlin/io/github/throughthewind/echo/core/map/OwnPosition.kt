package io.github.throughthewind.echo.core.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The user's own position from the platform `LocationManager` (spec FR-2.5): no Play Services, held
 * in memory while the map shows it, never stored, never sent, never logged. Empty when location
 * permission is not granted; asking for it is the screens' job (WI-013).
 */
object OwnPosition {
    private const val MIN_INTERVAL_MILLIS = 5_000L
    private const val MIN_DISTANCE_METERS = 5f

    fun updates(context: Context): Flow<OwnFix> {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyFlow()
        val manager = context.getSystemService(LocationManager::class.java) ?: return emptyFlow()
        return callbackFlow {
            val listener = LocationListener { fix -> trySend(OwnFix(fix.latitude, fix.longitude, fix.accuracy)) }
            try {
                manager.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    MIN_INTERVAL_MILLIS,
                    MIN_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper(),
                )
            } catch (_: IllegalArgumentException) {
                // No fused provider on this phone: GPS alone.
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_INTERVAL_MILLIS,
                    MIN_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper(),
                )
            } catch (_: SecurityException) {
                close()
            }
            awaitClose { manager.removeUpdates(listener) }
        }
    }
}
