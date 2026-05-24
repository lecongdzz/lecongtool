package com.lecongtool.proapp

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SecurityUtils.performSecurityChecks(this)
    }
}
