package io.github.throughthewind.echo.mesh

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.MainActivity
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.mesh.node.Sharing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import io.github.throughthewind.echo.core.designsystem.R as DS

/**
 * The `connectedDevice` foreground service that keeps the mesh relaying with the screen off (spec
 * FR-3.3, AC-5). It holds no radio itself: it only keeps the process alive while the mesh is on, and
 * shows the one notification Android requires (components/SystemNotification). Started only by
 * [App.meshOn], from a user's tap with the app visible; never restarted by Android (not sticky) and
 * never at boot.
 */
class MeshService : Service() {
    private var watching: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val app = application as App
        if (intent?.action == ACTION_TURN_OFF) {
            app.scope.launch { app.meshOff() }
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_STOP_SHARING) {
            // One tap, no confirm (FR-10.4); the mesh stays on.
            app.mesh.sharing.value
                .forEach { app.mesh.stopSharing(it.groupId) }
            return START_NOT_STICKY
        }
        createChannel()
        val started =
            runCatching {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification(app.mesh.state.value, app.mesh.sharing.value),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            }.isSuccess
        if (!started) {
            // Android refused (no Bluetooth permission, or not started from the foreground): the mesh
            // still runs while Echo is open, and the chip turns to "Relaying stopped" if it is killed.
            stopSelf()
            return START_NOT_STICKY
        }
        if (watching == null) watching = app.scope.launch { keepNotificationCurrent(app) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        watching?.cancel()
        watching = null
        super.onDestroy()
    }

    /** At most one update every 30 s, so the shade doesn't flicker in a pocket. */
    private suspend fun keepNotificationCurrent(app: App) {
        // While sharing, the time left changes every minute, so a tick keeps "stops in" honest.
        val tick =
            flow {
                while (true) {
                    emit(Unit)
                    delay(UPDATE_EVERY)
                }
            }
        combine(app.mesh.state.map { (it as? MeshState.On)?.nearby }, app.mesh.sharing, tick) { nearby, sharing, _ -> nearby to sharing }
            .distinctUntilChanged { a, b -> a == b && b.second.isEmpty() }
            .conflate()
            .collect { (nearby, sharing) ->
                if (nearby != null && NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    runCatching {
                        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(app.mesh.state.value, sharing))
                    }
                }
                delay(UPDATE_EVERY)
            }
    }

    private fun createChannel() {
        val channel =
            NotificationChannel(CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * The mesh line, or while sharing the sharing line (components/SystemNotification): "Sharing your
     * position with Crew", "Updates while Echo is open · stops in 1 h 42", with Stop sharing.
     */
    private fun notification(
        state: MeshState,
        sharing: List<Sharing>,
    ) = NotificationCompat
        .Builder(this, CHANNEL)
        .setSmallIcon(if (sharing.isEmpty()) DS.drawable.ic_radar else DS.drawable.ic_share_location)
        .setContentTitle(
            if (sharing.isEmpty()) {
                getString(R.string.notification_title)
            } else {
                getString(R.string.notification_sharing_title, sharing.joinToString(", ") { it.groupName })
            },
        ).setContentText(
            when {
                sharing.isNotEmpty() -> getString(R.string.notification_sharing_text, stopsIn(sharing))
                state is MeshState.On && state.nearby > 0 -> getString(R.string.notification_nearby, state.nearby.toString())
                else -> getString(R.string.notification_searching)
            },
        ).apply {
            if (sharing.isNotEmpty()) {
                addAction(
                    0,
                    getString(R.string.notification_stop_sharing),
                    PendingIntent.getService(
                        this@MeshService,
                        2,
                        Intent(this@MeshService, MeshService::class.java).setAction(ACTION_STOP_SHARING),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
        }.setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        // Nothing about the mesh shows on a locked phone (components/SystemNotification).
        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        .setContentIntent(
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
        ).addAction(
            0,
            getString(R.string.notification_turn_off),
            PendingIntent.getService(
                this,
                1,
                Intent(this, MeshService::class.java).setAction(ACTION_TURN_OFF),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

    /** "stops in 1 h 42", for the share that ends first; "until the mesh is off" when none has an end. */
    private fun stopsIn(sharing: List<Sharing>): String {
        val until = sharing.mapNotNull { it.until }.minOrNull() ?: return getString(R.string.sharing_until_off)
        val minutes = (until - Clock.System.now()).inWholeMinutes.coerceAtLeast(1).toInt()
        return if (minutes >= 60) {
            getString(R.string.notification_stops_in, minutes / 60, minutes % 60)
        } else {
            getString(R.string.notification_stops_in_minutes, minutes)
        }
    }

    companion object {
        private const val CHANNEL = "mesh"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TURN_OFF = "turn_off"
        private const val ACTION_STOP_SHARING = "stop_sharing"
        private val UPDATE_EVERY = 30.seconds

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, MeshService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MeshService::class.java))
        }
    }
}
