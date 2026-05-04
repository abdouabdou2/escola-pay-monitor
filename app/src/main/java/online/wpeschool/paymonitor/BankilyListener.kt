package online.wpeschool.paymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

class BankilyListener : NotificationListenerService() {

    companion object {
        const val BANKILY_PACKAGE = "mr.bpm.digitalbanking.consumer"
        const val ACTION_NEW_TRANSACTION = "online.wpeschool.paymonitor.NEW_TRANSACTION"
        private const val TAG = "BankilyListener"
        private const val DEBUG_CHANNEL_ID = "bankily_debug"
        private const val DEBUG_NOTIF_ID = 9_001
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val body = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d(TAG, "onNotificationPosted pkg=$pkg title=$title text=$body")

        if (pkg == BANKILY_PACKAGE) {
            showDebugNotification(body)
        }

        if (pkg != BANKILY_PACKAGE) return

        val tx = BankilyParser.parse(title, body)
        if (tx == null) {
            Log.d(TAG, "parse() returned null — forwarding raw text")
        }

        val txnId = tx?.transactionId ?: System.currentTimeMillis().toString()
        val sender = tx?.senderName ?: ""
        val amount = tx?.amount ?: body.take(200)
        val phone = tx?.phone ?: ""

        val intent = Intent(applicationContext, MonitorService::class.java).apply {
            action = ACTION_NEW_TRANSACTION
            putExtra(MonitorService.EXTRA_TXN_ID, txnId)
            putExtra(MonitorService.EXTRA_SENDER, sender)
            putExtra(MonitorService.EXTRA_AMOUNT, amount)
            putExtra(MonitorService.EXTRA_PHONE, phone)
        }
        startService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    private fun showDebugNotification(body: String) {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(DEBUG_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(DEBUG_CHANNEL_ID, "Bankily Debug", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notif = NotificationCompat.Builder(this, DEBUG_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📡 Bankily إشعار مُلتقط")
            .setContentText(body.take(100))
            .setAutoCancel(true)
            .build()
        nm.notify(DEBUG_NOTIF_ID, notif)
    }
}
