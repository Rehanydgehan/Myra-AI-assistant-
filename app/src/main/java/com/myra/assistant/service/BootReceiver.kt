package com.myra.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check permissions and start services if allowed.
            // On modern Android, starting background services from boot receiver is restricted.
            // We can start CallMonitorService if it's considered safe or request user to open app once.
            try {
                context.startService(Intent(context, CallMonitorService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
