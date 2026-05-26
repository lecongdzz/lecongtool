package com.lecongtool.proapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AutoClickService : AccessibilityService() {

    private val TARGET_PACKAGE = "com.ttboost.tik.tok.followers.likes"
    private var isRunning = false
    private var isJobRunning = false
    
    // Sử dụng Handler nguyên bản thay cho Coroutines
    private val handler = Handler(Looper.getMainLooper())

    private val autoClickRunnable = object : Runnable {
        override fun run() {
            if (isRunning && isJobRunning) {
                performClick(500f, 1000f)
                val randomDelay = (1000..2500).random().toLong()
                handler.postDelayed(this, randomDelay)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("ACTION")) {
            "START_TYM" -> {
                isRunning = true
                sendLogToUI("<font color='#00FF00'>[+] Khởi động Auto...</font>")
                sendLogToUI("<font color='#FFFFFF'>[*] Đang nhận diện nhiệm vụ...</font>")
            }
            "STOP" -> {
                isRunning = false
                isJobRunning = false
                handler.removeCallbacks(autoClickRunnable)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRunning || event == null) return
        
        val currentPackage = event.packageName?.toString()
        
        if (currentPackage != TARGET_PACKAGE) {
            sendLogToUI("<font color='#FFA500'>[⚠] Không tìm thấy App TTBoost!</font>")
            isJobRunning = false
            handler.removeCallbacks(autoClickRunnable)
        } else {
            if (!isJobRunning) {
                startAutoTymJob()
            }
        }
    }

    private fun startAutoTymJob() {
        isJobRunning = true
        sendLogToUI("<font color='#00FF00'>[+] Đã tìm thấy TTBoost. Tiến hành Auto Tym!</font>")
        handler.post(autoClickRunnable)
    }

    private fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun sendLogToUI(message: String) {
        val intent = Intent("TOOL_LOG_BROADCAST")
        intent.putExtra("LOG_MESSAGE", message)
        sendBroadcast(intent)
    }

    override fun onInterrupt() {
        isRunning = false
        isJobRunning = false
        handler.removeCallbacks(autoClickRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isJobRunning = false
        handler.removeCallbacks(autoClickRunnable)
    }
}
