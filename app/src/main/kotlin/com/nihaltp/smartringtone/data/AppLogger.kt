package com.nihaltp.smartringtone.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val PREFS_NAME = "AppLoggerPrefs"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"
    private const val LOG_FILE_NAME = "app.log"
    private const val OLD_LOG_FILE_NAME = "app.log.old"
    private const val MAX_FILE_SIZE = 512 * 1024 // 512 KB

    fun isLoggingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOGGING_ENABLED, false)
    }

    fun setLoggingEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply()
        if (!enabled) {
            clearLogs(context)
        }
    }

    @Synchronized
    fun log(
        context: Context,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (!isLoggingEnabled(context)) return

        val logFile = File(context.filesDir, LOG_FILE_NAME)

        // Rotation check
        if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
            val oldLogFile = File(context.filesDir, OLD_LOG_FILE_NAME)
            if (oldLogFile.exists()) {
                oldLogFile.delete()
            }
            logFile.renameTo(oldLogFile)
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            val timestamp = sdf.format(Date())
            val formatted =
                buildString {
                    append("$timestamp [$tag] $message\n")
                    if (throwable != null) {
                        append(android.util.Log.getStackTraceString(throwable))
                        append("\n")
                    }
                }
            FileOutputStream(logFile, true).use { fos ->
                fos.write(formatted.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun getLogs(context: Context): String {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        val oldLogFile = File(context.filesDir, OLD_LOG_FILE_NAME)

        val sb = java.lang.StringBuilder()

        // If old file exists, read it first (to keep chronological order)
        if (oldLogFile.exists()) {
            try {
                sb.append(oldLogFile.readText())
            } catch (e: Exception) {
                sb.append("Error reading older logs: ${e.message}\n")
            }
        }

        if (logFile.exists()) {
            try {
                sb.append(logFile.readText())
            } catch (e: Exception) {
                sb.append("Error reading active logs: ${e.message}\n")
            }
        }

        return sb.toString()
    }

    @Synchronized
    fun clearLogs(context: Context) {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        val oldLogFile = File(context.filesDir, OLD_LOG_FILE_NAME)
        if (logFile.exists()) logFile.delete()
        if (oldLogFile.exists()) oldLogFile.delete()
    }
}
