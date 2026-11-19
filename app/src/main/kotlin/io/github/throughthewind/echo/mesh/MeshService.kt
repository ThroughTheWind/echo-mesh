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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        createChannel()
        val started =
            runCatching {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification(app.mesh.state.value),
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
        app.mesh.state
            .map { (it as? MeshState.On)?.nearby }
            .distinctUntilChanged()
            .conflate()
            .collect { nearby ->
                if (nearby != null && NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    runCatching { NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(app.mesh.state.value)) }
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

    private fun notification(state: MeshState) =
        NotificationCompat
            .Builder(this, CHANNEL)
            .setSmallIcon(DS.drawable.ic_radar)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(
                when {
                    state is MeshState.On && state.nearby > 0 -> getString(R.string.notification_nearby, state.nearby.toString())
                    else -> getString(R.string.notification_searching)
                },
            ).setOngoing(true)
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

    companion object {
        private const val CHANNEL = "mesh"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TURN_OFF = "turn_off"
        private val UPDATE_EVERY = 30.seconds

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, MeshService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MeshService::class.java))
        }
    }
}
