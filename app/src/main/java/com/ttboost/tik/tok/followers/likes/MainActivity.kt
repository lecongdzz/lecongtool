// app/src/main/java/com/lecongtool/proapp/MainActivity.kt
package com.lecongtool.proapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnOnlyFollow: Button
    private lateinit var btnOnlyLike: Button
    private lateinit var btnTotal: Button
    private lateinit var tvLogs: TextView
    private lateinit var btnEmergencyStop: Button

    private var isRunning = false
    private var jobMode = "TOTAL"
    private var earnedCoins = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    private val tiktokLoadDelay = 2500L
    private val returnDelay = 800L
    
    // Gói ứng dụng mục tiêu cần làm nhiệm vụ (TTBoost)
    private val targetPkgName = "com.ttboost.tik.tok.followers.likes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOnlyFollow = findViewById(R.id.btnOnlyFollow)
        btnOnlyLike = findViewById(R.id.btnOnlyLike)
        btnTotal = findViewById(R.id.btnTotal)
        tvLogs = findViewById(R.id.tvLogs)
        btnEmergencyStop = findViewById(R.id.btnEmergencyStop)

        setupButtons()
    }

    private fun setupButtons() {
        btnOnlyFollow.setOnClickListener {
            jobMode = "FOLLOW"
            log("[*] CHẾ ĐỘ: Chỉ Follow (Tự động Skip Tym)", "#00FFFF")
            startAutomation()
        }

        btnOnlyLike.setOnClickListener {
            jobMode = "LIKE"
            log("[*] CHẾ ĐỘ: Chỉ Tym (Tự động Skip Follow)", "#00FFFF")
            startAutomation()
        }

        btnTotal.setOnClickListener {
            jobMode = "TOTAL"
            log("[*] CHẾ ĐỘ: Tổng Lực", "#00FFFF")
            startAutomation()
        }

        btnEmergencyStop.setOnClickListener {
            isRunning = false
            log("[!] ⏹ DỪNG TOOL KHẨN CẤP THÀNH CÔNG", "#FF0000")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun startAutomation() {
        // BẮT BUỘC KIỂM TRA QUYỀN TRỢ NĂNG (ACCESSIBILITY SERVICE)
        if (AutoClickService.instance == null) {
            log("[!] Vui lòng bật quyền Trợ Năng cho ứng dụng này trong Cài Đặt!", "#FF0000")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        
        if (!isRunning) {
            isRunning = true
            log("[+] Khởi động hệ thống Auto (Bypass ADB)...", "#00CC00")
            
            // Kích hoạt gọi ứng dụng mục tiêu lên màn hình
            launchTargetApp()

            // Tách luồng quét Job vào Background Thread để tránh đóng băng giao diện
            Thread { runAutomationLoop() }.start()
        }
    }

    private fun launchTargetApp() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPkgName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
                log("[+] Đã chuyển sang ứng dụng nhiệm vụ...", "#00FF00")
            } else {
                log("[⚠] Không tìm thấy App mục tiêu trên điện thoại!", "#FFA500")
            }
        } catch (e: Exception) {
            log("[⚠] Lỗi mở App: ${e.message}", "#FFA500")
        }
    }

    private fun log(message: String, colorHex: String) {
        mainHandler.post {
            tvLogs.append("\n<font color='$colorHex'>$message</font>")
            tvLogs.text = android.text.Html.fromHtml(tvLogs.text.toString(), android.text.Html.FROM_HTML_MODE_LEGACY)
            val scrollAmount = tvLogs.layout.getLineTop(tvLogs.lineCount) - tvLogs.height
            if (scrollAmount > 0) tvLogs.scrollTo(0, scrollAmount)
        }
    }

    private fun runAutomationLoop() {
        val service = AutoClickService.instance
        if (service == null) {
            isRunning = false
            return
        }

        while (isRunning) {
            log("[*] Đang nhận diện nhiệm vụ...", "#FFFFFF")
            var currentJob = ""

            var scanRetries = 0
            while (isRunning) {
                scanRetries++
                
                // Kéo giãn luồng nếu tool bị kẹt ở màn hình cũ
                if (scanRetries % 15 == 0) {
                    launchTargetApp()
                }

                if (service.checkTextExists("completed the task", "Error", "Unfollow after completing")) {
                    val skipCoords = service.findCoordinatesByText("Skip", "Bỏ qua")
                    if (skipCoords != null) service.tap(skipCoords.first, skipCoords.second)
                    log("[⚠] Lỗi Job hoặc đã làm -> ĐÃ SKIP!", "#FFA500")
                    Thread.sleep(1000)
                    continue
                }

                val followCoords = service.findCoordinatesByText("Follow +", "Theo dõi +")
                val likeCoords = service.findCoordinatesByText("Like +", "Thích +")
                val skipCoords = service.findCoordinatesByText("Skip", "Bỏ qua")
                
                var jobFound = false
                
                if (jobMode == "FOLLOW") {
                    if (likeCoords != null) {
                        if (skipCoords != null) service.tap(skipCoords.first, skipCoords.second)
                        log("-> Sai loại Job (Tym) -> ĐÃ SKIP!", "#FFA500")
                        Thread.sleep(1000)
                        continue
                    }
                    if (followCoords != null) {
                        service.tap(followCoords.first, followCoords.second)
                        currentJob = "follow"
                        jobFound = true
                    }
                } else if (jobMode == "LIKE") {
                    if (followCoords != null) {
                        if (skipCoords != null) service.tap(skipCoords.first, skipCoords.second)
                        log("-> Sai loại Job (Follow) -> ĐÃ SKIP!", "#FFA500")
                        Thread.sleep(1000)
                        continue
                    }
                    if (likeCoords != null) {
                        service.tap(likeCoords.first, likeCoords.second)
                        currentJob = "like"
                        jobFound = true
                    }
                } else if (jobMode == "TOTAL") {
                    if (followCoords != null) {
                        service.tap(followCoords.first, followCoords.second)
                        currentJob = "follow"
                        jobFound = true
                    } else if (likeCoords != null) {
                        service.tap(likeCoords.first, likeCoords.second)
                        currentJob = "like"
                        jobFound = true
                    }
                }

                if (jobFound) break else Thread.sleep(500)
            }

            if (!isRunning) break
            log("[+] Vào TikTok xử lý...", "#00FFFF")
            Thread.sleep(tiktokLoadDelay)

            if (currentJob == "like") {
                service.tap(540, 960)
                Thread.sleep(80)
                service.tap(540, 960)
                log("-> Đã thả Tym!", "#00FF00")
                Thread.sleep(1000)
            } else if (currentJob == "follow") {
                val coords = service.findCoordinatesByText("Follow", "Theo dõi", "Follow button")
                if (coords != null) {
                    service.tap(coords.first, coords.second)
                } else {
                    service.tap(900, 800)
                }
                log("-> Đã Follow!", "#00FF00")
                Thread.sleep(1000)
            }

            if (!isRunning) break
            
            // Gọi lại app mục tiêu sau khi xử lý xong TikTok
            launchTargetApp()
            Thread.sleep(returnDelay)
            
            earnedCoins += 10
            log("[] []Hoàn thành! Đã chạy: $earnedCoins xu[] []", "#BF00FF")
        }
    }
}
