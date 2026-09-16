package com.advol.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.advol.app.AdVolApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile allowing users to toggle AdVol on/off from the Android notification shade.
 */
@RequiresApi(Build.VERSION_CODES.N)
class AdVolTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? AdVolApplication ?: return

        serviceScope.launch {
            val isEnabled = app.preferencesManager.isServiceEnabled.first()
            val newState = !isEnabled
            app.preferencesManager.setServiceEnabled(newState)

            if (newState) {
                AdVolService.start(applicationContext)
            } else {
                app.volumeController.restore()
            }

            updateTileState(newState)
        }
    }

    private fun updateTileState(forcedState: Boolean? = null) {
        val tile = qsTile ?: return
        val app = applicationContext as? AdVolApplication ?: return

        serviceScope.launch {
            val isEnabled = forcedState ?: app.preferencesManager.isServiceEnabled.first()
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = if (isEnabled) "AdVol Active" else "AdVol Paused"
            tile.updateTile()
        }
    }
}
