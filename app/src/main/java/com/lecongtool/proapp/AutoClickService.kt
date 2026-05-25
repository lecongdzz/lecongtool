package com.lecongtool.proapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent

class AutoClickService : AccessibilityService() {
    companion object {
        var instance: AutoClickService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    
    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun tap(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 50L)).build()
        dispatchGesture(gesture, null, null)
    }

    fun findCoordinatesByText(vararg keywords: String): Pair<Int, Int>? {
        val root = rootInActiveWindow ?: return null
        for (keyword in keywords) {
            val nodes = root.findAccessibilityNodeInfosByText(keyword)
            for (node in nodes) {
                if (node.text?.toString()?.contains(keyword, ignoreCase = true) == true || 
                    node.contentDescription?.toString()?.contains(keyword, ignoreCase = true) == true) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    return Pair(rect.centerX(), rect.centerY())
                }
            }
        }
        return null
    }

    fun checkTextExists(vararg keywords: String): Boolean {
        val root = rootInActiveWindow ?: return false
        for (keyword in keywords) {
            val nodes = root.findAccessibilityNodeInfosByText(keyword)
            for (node in nodes) {
                if (node.text?.toString()?.contains(keyword, ignoreCase = true) == true || 
                    node.contentDescription?.toString()?.contains(keyword, ignoreCase = true) == true) {
                    return true
                }
            }
        }
        return false
    }
}
