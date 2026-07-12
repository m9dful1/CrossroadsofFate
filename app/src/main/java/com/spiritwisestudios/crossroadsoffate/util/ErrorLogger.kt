package com.spiritwisestudios.crossroadsoffate.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for error logging and crash reporting.
 * Provides methods to log different types of errors and save them to local storage.
 */
object ErrorLogger {

    private const val LOG_FILE_NAME = "error_log.txt"
    private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
    private const val ENTRY_SEPARATOR = "\n-------------------------------------------------\n"

    // Serializes file access: concurrent writers (e.g. two failing coroutines
    // logging at once) must not interleave partial entries
    private val fileMutex = Mutex()
    
    /**
     * Log an exception with a custom message
     * @param throwable The exception to log
     * @param message Additional context message (optional)
     */
    fun logException(throwable: Throwable, message: String? = null) {
        if (message != null) {
            Timber.e(throwable, message)
        } else {
            Timber.e(throwable)
        }
    }
    
    /**
     * Log an error message
     * @param message The error message
     * @param tag Optional tag for filtering logs
     */
    fun logError(message: String, tag: String? = null) {
        if (tag != null) {
            Timber.tag(tag).e(message)
        } else {
            Timber.e(message)
        }
    }
    
    /**
     * Log information message
     * @param message The info message
     */
    fun logInfo(message: String) {
        Timber.i(message)
    }
    
    /**
     * Save error to a file in the app's private storage.
     * Useful for collecting logs that can be sent later or viewed in-app.
     * File I/O runs on Dispatchers.IO so callers may invoke this from any dispatcher.
     *
     * @param context Application context
     * @param error The error message to save
     * @param throwable Optional exception to include in the log
     */
    suspend fun saveErrorToFile(context: Context, error: String, throwable: Throwable? = null): Unit = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val errorMessage = StringBuilder()
                .append("[$timestamp] ERROR: $error")
                .apply {
                    if (throwable != null) {
                        append("\nException: ${throwable.javaClass.simpleName}")
                        append("\nMessage: ${throwable.message}")
                        append("\nStack trace:\n")
                        throwable.stackTrace.take(10).forEach { element ->
                            append("    at $element\n")
                        }
                    }
                }
                .append(ENTRY_SEPARATOR)
                .toString()

            fileMutex.withLock {
                val logFile = File(context.filesDir, LOG_FILE_NAME)

                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                    // Drop the oldest half, but cut on an entry boundary so the
                    // surviving log doesn't start with a corrupt half-record
                    val content = logFile.readText()
                    val midpoint = content.length / 2
                    val boundary = content.indexOf(ENTRY_SEPARATOR, midpoint)
                    val cutAt = if (boundary >= 0) boundary + ENTRY_SEPARATOR.length else midpoint
                    logFile.writeText(content.substring(cutAt))
                }

                FileWriter(logFile, true).use { writer ->
                    writer.append(errorMessage)
                }
            }

            Timber.d("Error saved to file: $LOG_FILE_NAME")
        } catch (e: Exception) {
            // WARN, not ERROR: FileLoggingTree persists ERROR logs through this
            // method, so failing at ERROR level here would recurse forever
            Timber.w(e, "Failed to save error to file")
        }
    }
    
    /**
     * Get the entire error log content. Reads on Dispatchers.IO.
     * @param context Application context
     * @return The content of the error log or null if not found
     */
    suspend fun getErrorLog(context: Context): String? = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                null
            }
        }
    }

    /**
     * Clear the error log file. Deletes on Dispatchers.IO.
     * @param context Application context
     */
    suspend fun clearErrorLog(context: Context): Unit = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) {
                if (logFile.delete()) {
                    Timber.i("Error log file cleared")
                } else {
                    Timber.w("Failed to delete error log file")
                }
            }
        }
    }
} 