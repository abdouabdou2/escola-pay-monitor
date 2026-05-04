package online.wpeschool.paymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val trigger = intent.action == Intent.ACTION_BOOT_COMPLETED
                || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
                || intent.action == "android.intent.action.QUICKBOOT_POWERON"
        if (!trigger) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, MonitorService::class.java)
        )
    }
}
