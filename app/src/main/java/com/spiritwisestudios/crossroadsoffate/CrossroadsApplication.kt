package com.spiritwisestudios.crossroadsoffate

import android.app.Application
import android.content.pm.ApplicationInfo
import timber.log.Timber

/**
 * Custom Application class for CrossroadsOfFate app.
 * Handles global initialization of components including Timber for logging.
 */
class CrossroadsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            // Plant debug tree only in debug builds
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant crash reporting tree in release builds
            Timber.plant(CrashReportingTree())
        }
    }
    
    /**
     * Custom Timber tree for release builds.
     * Only logs errors and sends them to crash reporting service.
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // In a real application, this would send the error to a crash reporting service
                // Example: Crashlytics.log(priority, tag, message)
                // Example: if (t != null) Crashlytics.logException(t)
                
                // For now, just log to Android's log system
                android.util.Log.e(tag, message, t)
            }
        }
    }
} 