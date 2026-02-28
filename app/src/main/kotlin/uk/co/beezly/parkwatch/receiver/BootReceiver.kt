package uk.co.beezly.parkwatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import uk.co.beezly.parkwatch.service.TimerForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed — Bluetooth receiver will auto-register via manifest")
            // If a timer was running before boot (unlikely with foreground service but just in case)
            // we simply reset state — user will need to reconnect to their car to start fresh
            TimerForegroundService.isRunning = false
        }
    }
}
