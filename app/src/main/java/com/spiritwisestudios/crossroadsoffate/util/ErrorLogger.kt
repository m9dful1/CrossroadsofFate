package com.spiritwisestudios.crossroadsoffate.util

import android.content.Context
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
     * Log a warning message
     * @param message The warning message
     */
    fun logWarning(message: String) {
        Timber.w(message)
    }
    
    /**
     * Log debug information
     * @param message The debug message
     */
    fun logDebug(message: String) {
        Timber.d(message)
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
     * 
     * @param context Application context
     * @param error The error message to save
     * @param throwable Optional exception to include in the log
     */
    fun saveErrorToFile(context: Context, error: String, throwable: Throwable? = null) {
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
                .append("\n-------------------------------------------------\n")
                .toString()
            
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            
            // Check if file exists and its size
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                // If file is too large, truncate it by removing the oldest entries
                val content = logFile.readText()
                val truncatedContent = content.substring(content.length / 2)
                logFile.writeText(truncatedContent)
            }
            
            // Append the error to the log file
            FileWriter(logFile, true).use { writer ->
                writer.append(errorMessage)
            }
            
            Timber.d("Error saved to file: $LOG_FILE_NAME")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save error to file")
        }
    }
    
    /**
     * Get the entire error log content
     * @param context Application context
     * @return The content of the error log or null if not found
     */
    fun getErrorLog(context: Context): String? {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        return if (logFile.exists()) {
            logFile.readText()
        } else {
            null
        }
    }
    
    /**
     * Clear the error log file
     * @param context Application context
     */
    fun clearErrorLog(context: Context) {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        if (logFile.exists()) {
            logFile.delete()
            Timber.i("Error log file cleared")
        }
    }
} 