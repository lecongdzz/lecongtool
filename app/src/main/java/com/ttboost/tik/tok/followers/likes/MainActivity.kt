package com.lecongtool.proapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("LOG_MESSAGE")
            if (message != null) {
                appendLog(message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLogStatus)
        val btnChiTym = findViewById<Button>(R.id.btnChiTym)
        val btnStop = findViewById<Button>(R.id.btnStopEmergency)

        registerReceiver(logReceiver, IntentFilter("TOOL_LOG_BROADCAST"), RECEIVER_EXPORTED)

        btnChiTym.setOnClickListener {
            appendLog("<font color='#00FFFF'>[*] CHẾ ĐỘ: Chỉ Tym</font>")
            if (!isAccessibilityServiceEnabled(this, AutoClickService::class.java)) {
                appendLog("<font color='#FF0000'>[!] Vui lòng bật quyền Trợ Năng cho Tool!</font>")
            } else {
                val serviceIntent = Intent(this, AutoClickService::class.java)
                serviceIntent.putExtra("ACTION", "START_TYM")
                startService(serviceIntent)
            }
        }

        btnStop.setOnClickListener {
            val serviceIntent = Intent(this, AutoClickService::class.java)
            serviceIntent.putExtra("ACTION", "STOP")
            startService(serviceIntent)
            appendLog("<font color='#FF0000'>[!] ĐÃ DỪNG TOOL KHẨN CẤP</font>")
        }
    }

    private fun appendLog(htmlMessage: String) {
        runOnUiThread {
            val currentText = tvLog.text.toString()
            // Dùng Html.fromHtml để hiển thị màu chữ theo thẻ <font>
            tvLog.append(Html.fromHtml("<br>$htmlMessage", Html.FROM_HTML_MODE_LEGACY))
        }
    }

    // Hàm check xem quyền Trợ năng đã được bật chưa
    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out android.accessibilityservice.AccessibilityService>): Boolean {
        val prefString = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains(context.packageName + "/" + service.name) == true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }
}
