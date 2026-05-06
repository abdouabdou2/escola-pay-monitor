package com.eschool24.paymonitor

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnEnable: Button
    private lateinit var btnTest: Button
    private lateinit var tvTestResult: TextView

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled in onResume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnEnable = findViewById(R.id.btnEnable)
        btnTest = findViewById(R.id.btnTest)
        tvTestResult = findViewById(R.id.tvTestResult)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnTest.setOnClickListener { sendTestPayload() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(perm)
            }
        }

        if (!isNotificationListenerEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun sendTestPayload() {
        tvTestResult.text = "Sending…"
        btnTest.isEnabled = false
        Log.d("MainActivity", "sendTestPayload triggered")
        CoroutineScope(Dispatchers.IO).launch {
            val intent = Intent(this@MainActivity, MonitorService::class.java).apply {
                action = BankilyListener.ACTION_NEW_TRANSACTION
                putExtra(MonitorService.EXTRA_TXN_ID, "TEST_${System.currentTimeMillis()}")
                putExtra(MonitorService.EXTRA_SENDER, "Test User")
                putExtra(MonitorService.EXTRA_AMOUNT, "100 MRU")
                putExtra(MonitorService.EXTRA_PHONE, "22000001")
            }
            ContextCompat.startForegroundService(this@MainActivity, intent)
            withContext(Dispatchers.Main) {
                tvTestResult.text = "Test payload sent — check server logs"
                btnTest.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val listenerEnabled = isNotificationListenerEnabled()

        if (listenerEnabled) {
            tvStatus.text = getString(R.string.status_active)
            btnEnable.isEnabled = false
            ContextCompat.startForegroundService(
                this,
                Intent(this, MonitorService::class.java)
            )
        } else {
            tvStatus.text = getString(R.string.status_inactive)
            btnEnable.isEnabled = true
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, BankilyListener::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabled?.contains(cn) == true
    }
}
