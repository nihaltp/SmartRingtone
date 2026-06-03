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
    const val ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER = "__DEFAULT__"

    private val gson = Gson()

    fun getRingtones(context: Context): List<Ringtone> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RINGTONES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Ringtone>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
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

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultVal = System.currentTimeMillis()
        val saved = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        if (saved == 0L) {
            prefs.edit().putLong(KEY_LAST_SYNC_TIME, defaultVal).apply()
            return defaultVal
        }
        return saved
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

    fun clearCallLogsHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CALL_LOGS).apply()
    }
}
