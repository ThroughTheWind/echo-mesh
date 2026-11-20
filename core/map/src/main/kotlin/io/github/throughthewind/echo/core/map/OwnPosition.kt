package io.github.throughthewind.echo.core.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The user's own position from the platform `LocationManager` (spec FR-2.5): no Play Services, held
 * in memory while a screen uses it, never stored, never logged, and sent only to a group the user
 * chose to share with, while sharing is on (FR-10). Empty when location
 * permission is not granted; asking for it is the screens' job (WI-013).
 */
object OwnPosition {
    private const val MIN_INTERVAL_MILLIS = 5_000L
    private const val MIN_DISTANCE_METERS = 5f
    private const val NANOS_PER_MILLI = 1_000_000L

    /** A last known fix older than this is not shown or shared as the current one. */
    private const val LAST_KNOWN_FRESH_MILLIS = 120_000L

    /**
     * Fixes from now on, starting with one right away: the last known fix if it is fresh, and a
     * current fix asked for once. A phone standing still otherwise may get none, because the update
     * request alone waits for movement or for the provider's own schedule (WI-075, found on a Pixel 9a
     * whose sharing service received no fix in 12 minutes). [minDistanceMeters] 0 keeps fixes coming
     * while standing still, which position sharing needs to stay fresh with the screen off.
     */
    fun updates(
        context: Context,
        minDistanceMeters: Float = MIN_DISTANCE_METERS,
    ): Flow<OwnFix> {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyFlow()
        val manager = context.getSystemService(LocationManager::class.java) ?: return emptyFlow()
        return callbackFlow {
            val send = { fix: Location -> trySend(OwnFix(fix.latitude, fix.longitude, fix.accuracy)) }
            val listener = LocationListener { fix -> send(fix) }
            val once = CancellationSignal()
            val provider =
                if (manager.hasProvider(LocationManager.FUSED_PROVIDER)) LocationManager.FUSED_PROVIDER else LocationManager.GPS_PROVIDER
            try {
                manager.getLastKnownLocation(provider)?.takeIf(::fresh)?.let { send(it) }
                manager.getCurrentLocation(provider, once, ContextCompat.getMainExecutor(context)) { fix -> fix?.let { send(it) } }
                manager.requestLocationUpdates(provider, MIN_INTERVAL_MILLIS, minDistanceMeters, listener, Looper.getMainLooper())
            } catch (_: IllegalArgumentException) {
                // The provider is missing or disabled on this phone: no fix, as without permission.
                close()
            } catch (_: SecurityException) {
                close()
            }
            awaitClose {
                once.cancel()
                manager.removeUpdates(listener)
            }
        }
    }

    private fun fresh(fix: Location): Boolean =
        SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos <= LAST_KNOWN_FRESH_MILLIS * NANOS_PER_MILLI
}
