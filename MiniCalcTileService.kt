package com.example.calculator

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

class MiniCalcTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_MINI_CALC"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivityAndCollapse(intent)
    }
}
