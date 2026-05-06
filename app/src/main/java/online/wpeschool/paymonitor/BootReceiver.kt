package com.eschool24.paymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
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

        if (!isNotificationListenerEnabled(context)) {
            showPermissionNotification(context)
        }
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, BankilyListener::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        return enabled?.contains(cn) == true
    }

    private fun showPermissionNotification(context: Context) {
        val channelId = "escola_boot"
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Boot Alerts", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, channelId)
            .setContentTitle("E-School · يحتاج صلاحية")
            .setContentText("اضغط لتفعيل صلاحية الإشعارات")
            .setSmallIcon(R.drawable.ic_monitor)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(1002, notif)
    }
}
