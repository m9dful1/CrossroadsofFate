package com.spiritwisestudios.crossroadsoffate.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Timber tree that persists ERROR-priority logs to the [ErrorLogger] file, so
 * the in-app error log shows real failures rather than only entries written
 * explicitly through [ErrorLogger.saveErrorToFile].
 *
 * Only ERROR is persisted: [ErrorLogger.saveErrorToFile] reports its own
 * failures at WARN, which keeps a failing log file from recursing through
 * this tree.
 */
class FileLoggingTree(
    context: Context,
    private val scope: CoroutineScope
) : Timber.Tree() {

    private val appContext = context.applicationContext

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority != Log.ERROR) return
        val entry = if (tag != null) "[$tag] $message" else message
        scope.launch {
            ErrorLogger.saveErrorToFile(appContext, entry, t)
        }
    }
}
