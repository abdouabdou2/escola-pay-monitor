package online.wpeschool.paymonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

class MonitorService : Service() {

    companion object {
        const val EXTRA_TXN_ID = "transaction_id"
        const val EXTRA_SENDER = "sender_name"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_PHONE = "phone"

        private const val CHANNEL_ID = "escola_monitor"
        private const val NOTIF_ID = 1001
        private const val SERVER_URL = "https://wpeschool.online/api/pay-notify"
        private const val HMAC_SECRET = "ESCOLA_PAY_2026"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == BankilyListener.ACTION_NEW_TRANSACTION) {
            val txnId = intent.getStringExtra(EXTRA_TXN_ID) ?: return START_STICKY
            val sender = intent.getStringExtra(EXTRA_SENDER) ?: ""
            val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: ""
            val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
            scope.launch { post(txnId, sender, amount, phone) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "E-School Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Bankily payment monitoring"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("E-School · جارٍ المراقبة 🟢")
            .setSmallIcon(R.drawable.ic_monitor)
            .setOngoing(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun hmacToken(): String {
        val timeSlot = floor(System.currentTimeMillis() / 60_000.0).toLong().toString()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(timeSlot.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun post(txnId: String, sender: String, amount: String, phone: String) {
        try {
            val payload = JSONObject().apply {
                put("transaction_id", txnId)
                put("sender_name", sender)
                put("amount", amount)
                put("phone", phone)
            }.toString().toByteArray(Charsets.UTF_8)

            val conn = (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Pay-Token", hmacToken())
                setRequestProperty("Content-Length", payload.size.toString())
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
            }

            conn.outputStream.use { it.write(payload) }
            val code = conn.responseCode
            conn.disconnect()

            // On 4xx/5xx, silently drop — the server may log duplicates on retry
            if (code in 500..599) {
                // Could add retry logic here if needed
            }
        } catch (_: Exception) {
            // Network failures are non-fatal; Bankily will not resend
        }
    }
}
