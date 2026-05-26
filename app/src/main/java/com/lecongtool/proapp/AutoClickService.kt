package com.lecongtool.proapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AutoClickService : AccessibilityService() {

    // CHÍNH XÁC PACKAGE CỦA APP TTBOOST
    private val TARGET_PACKAGE = "com.ttboost.tik.tok.followers.likes"
    private var isRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onServiceConnected() {
        super.onServiceConnected()
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
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRunning || event == null) return
        
        val currentPackage = event.packageName?.toString()
        
        // KIỂM TRA ĐÚNG APP ĐÍCH MỚI CHẠY AUTO
        if (currentPackage != TARGET_PACKAGE) {
            sendLogToUI("<font color='#FFA500'>[⚠] Không tìm thấy App TTBoost!</font>")
        } else {
            // Đã vào đúng TTBoost -> Bắt đầu click tim
            startAutoTymJob()
        }
    }

    private var isJobRunning = false
    private fun startAutoTymJob() {
        if (isJobRunning) return
        isJobRunning = true
        
        serviceScope.launch {
            sendLogToUI("<font color='#00FF00'>[+] Đã tìm thấy TTBoost. Tiến hành Auto Tym!</font>")
            while (isRunning) {
                // Tọa độ click Tym (Giả lập màn hình Oppo)
                performClick(500f, 1000f) 
                
                // Delay an toàn tránh dính spam
                delay((1000..2500).random().toLong()) 
            }
            isJobRunning = false
        }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
