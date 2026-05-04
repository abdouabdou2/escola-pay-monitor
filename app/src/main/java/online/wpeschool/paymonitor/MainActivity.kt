package online.wpeschool.paymonitor

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnEnable: Button

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled in onResume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnEnable = findViewById(R.id.btnEnable)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(perm)
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
