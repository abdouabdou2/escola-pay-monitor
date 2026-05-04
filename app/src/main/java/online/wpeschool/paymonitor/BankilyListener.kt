package online.wpeschool.paymonitor

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BankilyListener : NotificationListenerService() {

    companion object {
        const val BANKILY_PACKAGE = "mr.bpm.digitalbanking.consumer"
        const val ACTION_NEW_TRANSACTION = "online.wpeschool.paymonitor.NEW_TRANSACTION"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != BANKILY_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val body = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && body.isBlank()) return

        val tx = BankilyParser.parse(title, body) ?: return

        val intent = Intent(applicationContext, MonitorService::class.java).apply {
            action = ACTION_NEW_TRANSACTION
            putExtra(MonitorService.EXTRA_TXN_ID, tx.transactionId)
            putExtra(MonitorService.EXTRA_SENDER, tx.senderName)
            putExtra(MonitorService.EXTRA_AMOUNT, tx.amount)
            putExtra(MonitorService.EXTRA_PHONE, tx.phone)
        }
        startService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit
}
