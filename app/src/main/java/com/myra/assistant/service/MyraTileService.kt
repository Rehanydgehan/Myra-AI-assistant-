package com.myra.assistant.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MyraTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, Class.forName("com.myra.assistant.MainActivity"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
}
