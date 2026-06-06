package com.nihaltp.smartringtone.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PreferenceHelper {
    private const val PREFS_NAME = "RingtoneChangerPrefs"
    private const val KEY_RINGTONES = "ringtones"
    private const val KEY_SCORE_PREFIX = "score_"
    private const val KEY_ORIGINAL_RINGTONE_PREFIX = "orig_rt_"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    private const val KEY_CALL_LOGS = "call_logs_history"
    private const val KEY_FALLBACK_RINGTONE_URI = "fallback_ringtone_uri"
    private const val KEY_FALLBACK_RINGTONE_NAME = "fallback_ringtone_name"
    private const val KEY_APP_PAUSED = "app_paused"
    private const val KEY_BACKUP_FILE_URI = "backup_file_uri"
    private const val KEY_SCORE_ADDITION_MISSED = "score_addition_missed"
    private const val KEY_SCORE_ADDITION_REJECTED = "score_addition_rejected"
    private const val KEY_LOG_TAB_ENABLED = "log_tab_enabled"
    const val ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER = "__DEFAULT__"

    private val gson = Gson()

    fun getRingtones(context: Context): List<Ringtone> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RINGTONES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Ringtone>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            AppLogger.log(context, "PreferenceHelper", "getRingtones() failed to parse: $json", e)
            emptyList()
        }
    }

    fun saveRingtones(
        context: Context,
        ringtones: List<Ringtone>,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(ringtones)
        prefs.edit().putString(KEY_RINGTONES, json).apply()
    }

    fun getContactScore(
        context: Context,
        contactId: String,
    ): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SCORE_PREFIX + contactId, 0)
    }

    fun setContactScore(
        context: Context,
        contactId: String,
        score: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SCORE_PREFIX + contactId, score).apply()
    }

    fun getOriginalRingtone(
        context: Context,
        contactId: String,
    ): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ORIGINAL_RINGTONE_PREFIX + contactId, null)
    }

    fun setOriginalRingtone(
        context: Context,
        contactId: String,
        uriString: String?,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (uriString != null) {
            prefs.edit().putString(KEY_ORIGINAL_RINGTONE_PREFIX + contactId, uriString).apply()
        } else {
            prefs.edit().remove(KEY_ORIGINAL_RINGTONE_PREFIX + contactId).apply()
        }
    }

    fun clearContactData(
        context: Context,
        contactId: String,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_SCORE_PREFIX + contactId)
            .remove(KEY_ORIGINAL_RINGTONE_PREFIX + contactId)
            .apply()
    }

    fun getAllScores(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.filterKeys { it.startsWith(KEY_SCORE_PREFIX) }
            .map { (key, value) ->
                val contactId = key.substring(KEY_SCORE_PREFIX.length)
                contactId to ((value as? Number)?.toInt() ?: 0)
            }.toMap()
    }

    fun getAllOriginalRingtones(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.filterKeys { it.startsWith(KEY_ORIGINAL_RINGTONE_PREFIX) }
            .map { (key, value) ->
                val contactId = key.substring(KEY_ORIGINAL_RINGTONE_PREFIX.length)
                contactId to ((value as? String) ?: "")
            }.toMap()
    }

    fun clearAllContactScores(
        context: Context,
        contactIds: List<String>,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in contactIds) {
            editor.remove(KEY_SCORE_PREFIX + id)
            editor.remove(KEY_ORIGINAL_RINGTONE_PREFIX + id)
        }
        editor.apply()
    }

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun setLastSyncTime(
        context: Context,
        time: Long,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }

    fun getCallLogsHistory(context: Context): List<CallLogEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CALL_LOGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CallLogEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            AppLogger.log(context, "PreferenceHelper", "getCallLogsHistory() failed to parse: $json", e)
            emptyList()
        }
    }

    fun addCallLogEntry(
        context: Context,
        entry: CallLogEntry,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = getCallLogsHistory(context).toMutableList()
        history.add(0, entry)
        if (history.size > 50) {
            history.removeAt(history.size - 1)
        }
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_CALL_LOGS, json).apply()
    }

    fun addCallLogEntries(
        context: Context,
        entries: List<CallLogEntry>,
    ) {
        if (entries.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = getCallLogsHistory(context).toMutableList()
        for (entry in entries) {
            history.add(0, entry)
        }
        while (history.size > 50) {
            history.removeAt(history.size - 1)
        }
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_CALL_LOGS, json).apply()
    }

    private const val KEY_SCREENSHOT_MODE = "screenshot_mode"
    private const val KEY_THEME = "app_theme"

    fun clearCallLogsHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CALL_LOGS).apply()
    }

    fun saveCallLogsHistory(
        context: Context,
        history: List<CallLogEntry>,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_CALL_LOGS, json).apply()
    }

    fun clearCallLogsForContact(
        context: Context,
        contactId: String,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = getCallLogsHistory(context).toMutableList()
        val contactName = ContactHelper.getContactName(context, contactId)
        val updated =
            history.filter { entry ->
                val entryContactId = ContactHelper.getContactIdFromNumber(context, entry.number)
                val matchesId = entryContactId == contactId
                val matchesName = contactName.isNotEmpty() && entry.name == contactName
                !matchesId && !matchesName
            }
        val json = gson.toJson(updated)
        prefs.edit().putString(KEY_CALL_LOGS, json).apply()
    }

    fun isScreenshotMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SCREENSHOT_MODE, false)
    }

    fun setScreenshotMode(
        context: Context,
        enabled: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SCREENSHOT_MODE, enabled).apply()
    }

    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }

    fun setTheme(
        context: Context,
        theme: String,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getFallbackRingtoneUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FALLBACK_RINGTONE_URI, null)
    }

    fun getFallbackRingtoneName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FALLBACK_RINGTONE_NAME, null)
    }

    fun setFallbackRingtone(
        context: Context,
        uriString: String?,
        nameString: String?,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (uriString != null) {
            prefs.edit()
                .putString(KEY_FALLBACK_RINGTONE_URI, uriString)
                .putString(KEY_FALLBACK_RINGTONE_NAME, nameString)
                .apply()
        } else {
            prefs.edit()
                .remove(KEY_FALLBACK_RINGTONE_URI)
                .remove(KEY_FALLBACK_RINGTONE_NAME)
                .apply()
        }
    }

    fun isAppPaused(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_PAUSED, false)
    }

    fun setAppPaused(
        context: Context,
        paused: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_PAUSED, paused).apply()
    }

    fun isLogTabEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOG_TAB_ENABLED, true)
    }

    fun setLogTabEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOG_TAB_ENABLED, enabled).apply()
    }

    fun getBackupFileUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BACKUP_FILE_URI, null)
    }

    fun setBackupFileUri(
        context: Context,
        uriString: String?,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (uriString != null) {
            prefs.edit().putString(KEY_BACKUP_FILE_URI, uriString).apply()
        } else {
            prefs.edit().remove(KEY_BACKUP_FILE_URI).apply()
        }
    }

    fun getScoreAdditionMissed(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SCORE_ADDITION_MISSED, 1)
    }

    fun setScoreAdditionMissed(
        context: Context,
        value: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SCORE_ADDITION_MISSED, value).apply()
    }

    fun getScoreAdditionRejected(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SCORE_ADDITION_REJECTED, 2)
    }

    fun setScoreAdditionRejected(
        context: Context,
        value: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SCORE_ADDITION_REJECTED, value).apply()
    }

    fun exportPreferences(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allPrefs = prefs.all.toMutableMap()
        allPrefs.remove(KEY_BACKUP_FILE_URI)
        return gson.toJson(allPrefs)
    }

    fun importPreferences(
        context: Context,
        json: String,
    ): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = gson.fromJson(json, type) ?: return false

            // Restore all current custom ringtones in system DB first
            ContactHelper.restoreAllRingtonesToDefault(context)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val backupUri = prefs.getString(KEY_BACKUP_FILE_URI, null)

            val editor = prefs.edit()
            editor.clear()
            if (backupUri != null) {
                editor.putString(KEY_BACKUP_FILE_URI, backupUri)
            }

            for ((key, value) in map) {
                if (value == null) continue
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                    is Number -> {
                        if (key == KEY_LAST_SYNC_TIME) {
                            editor.putLong(key, value.toLong())
                        } else if (key.startsWith(KEY_SCORE_PREFIX) ||
                            key == KEY_SCORE_ADDITION_MISSED ||
                            key == KEY_SCORE_ADDITION_REJECTED
                        ) {
                            editor.putInt(key, value.toInt())
                        } else {
                            editor.putLong(key, value.toLong())
                        }
                    }
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            AppLogger.log(context, "PreferenceHelper", "importPreferences failed", e)
            false
        }
    }
}
