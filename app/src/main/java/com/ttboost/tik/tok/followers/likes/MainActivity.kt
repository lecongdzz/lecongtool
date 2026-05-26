package com.lecongtool.proapp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

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

        // Fix lỗi crash CI liên quan đến API 33+ (RECEIVER_EXPORTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, IntentFilter("TOOL_LOG_BROADCAST"), Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(logReceiver, IntentFilter("TOOL_LOG_BROADCAST"))
        }

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

    @Suppress("DEPRECATION")
    private fun appendLog(htmlMessage: String) {
        runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tvLog.append(Html.fromHtml("<br>$htmlMessage", Html.FROM_HTML_MODE_LEGACY))
            } else {
                tvLog.append(Html.fromHtml("<br>$htmlMessage"))
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val prefString = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains(context.packageName + "/" + service.name) == true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }
}
