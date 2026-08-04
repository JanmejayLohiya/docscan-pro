package com.docscan.pro

import android.app.Application
import com.docscan.pro.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScanProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
