package com.btcbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED &&
            c.getSharedPreferences("s", Context.MODE_PRIVATE).getBoolean("on", false)
        ) {
            c.startForegroundService(Intent(c, PriceService::class.java))
        }
    }
}
