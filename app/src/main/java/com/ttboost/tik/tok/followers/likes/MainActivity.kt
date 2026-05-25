package com.lecongtool.proapp

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

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
    private val pkgName = "com.lecongtool.proapp"

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
        if (!isRunning) {
            isRunning = true
            log("[+] Khởi động hệ thống Auto...", "#00CC00")
            Thread { runAutomationLoop() }.start()
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

    private fun executeRootCmd(cmd: String): String {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val isReader = BufferedReader(InputStreamReader(process.inputStream))
            os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()
            val output = StringBuilder()
            var line: String?
            while (isReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            return output.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun tapFast(x: Int, y: Int) {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "input tap $x $y &"))
    }

    private fun getScreenXml(): String {
        executeRootCmd("uiautomator dump /sdcard/v.xml")
        return executeRootCmd("cat /sdcard/v.xml")
    }

    private fun parseElementCoords(xmlData: String, keywords: List<String>): Pair<Int, Int>? {
        for (keyword in keywords) {
            val pattern = Pattern.compile("(text|content-desc)=\"[^\"]*\\Q$keyword\\E[^\"]*\".*?bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(xmlData)
            if (matcher.find()) {
                val x1 = matcher.group(2)!!.toInt()
                val y1 = matcher.group(3)!!.toInt()
                val x2 = matcher.group(4)!!.toInt()
                val y2 = matcher.group(5)!!.toInt()
                return Pair((x1 + x2) / 2, (y1 + y2) / 2)
            }
        }
        return null
    }

    private fun runAutomationLoop() {
        while (isRunning) {
            log("[*] Đang nhận diện nhiệm vụ...", "#FFFFFF")
            var currentJob = ""

            while (isRunning) {
                val xmlData = getScreenXml()
                val skipCoords = parseElementCoords(xmlData, listOf("Skip", "Bỏ qua"))

                // 1. Kiểm tra lỗi hệ thống hoặc job rác -> Skip ngay
                if (xmlData.contains("completed the task") || xmlData.contains("Error") || xmlData.contains("Unfollow after completing")) {
                    if (skipCoords != null) tapFast(skipCoords.first, skipCoords.second)
                    log("[⚠] Lỗi Job hoặc đã làm -> ĐÃ SKIP!", "#FFA500")
                    Thread.sleep(1000)
                    continue
                }

                val followCoords = parseElementCoords(xmlData, listOf("Follow +", "Theo dõi +"))
                val likeCoords = parseElementCoords(xmlData, listOf("Like +", "Thích +"))
                
                var jobFound = false
                
                // 2. Logic Lọc Job (Bắt buộc Skip nếu sai thể loại)
                if (jobMode == "FOLLOW") {
                    if (likeCoords != null) {
                        if (skipCoords != null) tapFast(skipCoords.first, skipCoords.second)
                        log("-> Sai loại Job (Tym) -> ĐÃ SKIP!", "#FFA500")
                        Thread.sleep(1000)
                        continue
                    }
                    if (followCoords != null) {
                        tapFast(followCoords.first, followCoords.second)
                        currentJob = "follow"
                        jobFound = true
                    }
                } else if (jobMode == "LIKE") {
                    if (followCoords != null) {
                        if (skipCoords != null) tapFast(skipCoords.first, skipCoords.second)
                        log("-> Sai loại Job (Follow) -> ĐÃ SKIP!", "#FFA500")
                        Thread.sleep(1000)
                        continue
                    }
                    if (likeCoords != null) {
                        tapFast(likeCoords.first, likeCoords.second)
                        currentJob = "like"
                        jobFound = true
                    }
                } else if (jobMode == "TOTAL") {
                    if (followCoords != null) {
                        tapFast(followCoords.first, followCoords.second)
                        currentJob = "follow"
                        jobFound = true
                    } else if (likeCoords != null) {
                        tapFast(likeCoords.first, likeCoords.second)
                        currentJob = "like"
                        jobFound = true
                    }
                }

                if (jobFound) break else Thread.sleep(500)
            }

            if (!isRunning) break
            Thread.sleep(tiktokLoadDelay)

            if (currentJob == "like") {
                tapFast(540, 960)
                Thread.sleep(80)
                tapFast(540, 960)
                Thread.sleep(1000)
            } else if (currentJob == "follow") {
                val tiktokXml = getScreenXml()
                val coords = parseElementCoords(tiktokXml, listOf("Follow", "Theo dõi", "Follow button"))
                if (coords != null) {
                    tapFast(coords.first, coords.second)
                } else {
                    tapFast(900, 800)
                }
                Thread.sleep(1000)
            }

            if (!isRunning) break
            Runtime.getRuntime().exec(arrayOf("su", "-c", "am start -n $pkgName/.MainActivity"))
            Thread.sleep(returnDelay)
            
            earnedCoins += 10
            log("[] []Hoàn thành! Đã chạy: $earnedCoins xu[] []", "#BF00FF")
        }
    }
}
