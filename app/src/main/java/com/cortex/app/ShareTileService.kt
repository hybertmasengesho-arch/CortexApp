package com.cortex.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * The tile that shows up in the phone's Quick Settings tray, next to
 * Bluetooth / Wi-Fi, once the user long-presses the tray and adds "Cortex
 * Files". Tapping it opens the app straight to the file upload screen.
 */
class ShareTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.state = Tile.STATE_INACTIVE
            it.label = getString(R.string.tile_label)
            it.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_FILES, true)
        }

        // Android 14+ requires collapsing the shade via a PendingIntent;
        // older versions can launch the Activity directly from here.
        if (Build.VERSION.SDK_INT >= 34) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(launchIntent)
        }
    }
}
