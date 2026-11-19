package io.github.throughthewind.echo.mesh

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import kotlinx.coroutines.launch

/**
 * The "Echo discreet" Quick Settings tile (FR-13.3, patterns/DiscreetMode): it toggles discreet mode
 * and shows On or Off, never a report, a count or a group. Settings are sealed under a key that needs
 * the phone unlocked, so on a locked phone a tap asks to unlock first, and the tile reads as
 * unavailable rather than guessing.
 */
class DiscreetTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label)
        tile.state =
            when {
                isLocked -> Tile.STATE_UNAVAILABLE

                (application as App)
                    .settings.settings.value.discreet -> Tile.STATE_ACTIVE

                else -> Tile.STATE_INACTIVE
            }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { toggle() } else toggle()
    }

    private fun toggle() {
        val app = application as App
        val on = !app.settings.settings.value.discreet
        app.scope.launch { app.setDiscreet(on) }
        qsTile?.let {
            it.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.updateTile()
        }
    }

    companion object {
        /** Asks the system to redraw the tile after discreet mode changed in the app. */
        fun refresh(context: Context) {
            runCatching { requestListeningState(context, ComponentName(context, DiscreetTile::class.java)) }
        }
    }
}
