package com.nihaltp.smartringtone.data

import android.content.Context
import android.provider.CallLog
import android.util.Log

object CallSyncHelper {
    fun syncCallLogs(context: Context) {
        val lastSyncTime = PreferenceHelper.getLastSyncTime(context)
        AppLogger.log(context, "CallSyncHelper", "syncCallLogs starting from lastSyncTime=$lastSyncTime")
        val contentResolver = context.contentResolver

        // Query all calls since lastSyncTime, sorted ASCENDING so we process them chronologically
        val projection =
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE,
            )
        val selection = "${CallLog.Calls.DATE} > ?"
        val selectionArgs = arrayOf(lastSyncTime.toString())
        val sortOrder = "${CallLog.Calls.DATE} ASC"

        try {
            val cursor =
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                )

            var newLastSyncTime = lastSyncTime

            cursor?.use {
                val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                val durCol = it.getColumnIndex(CallLog.Calls.DURATION)
                val dateCol = it.getColumnIndex(CallLog.Calls.DATE)

                while (it.moveToNext()) {
                    val number = it.getString(numCol) ?: ""
                    val type = it.getInt(typeCol)
                    val duration = it.getLong(durCol)
                    val date = it.getLong(dateCol)

                    if (date > newLastSyncTime) {
                        newLastSyncTime = date
                    }

                    // Process the call
                    processCall(context, number, type, duration, date)
                }
            }

            // Update the last sync time to now
            PreferenceHelper.setLastSyncTime(context, System.currentTimeMillis())
        } catch (e: SecurityException) {
            Log.e("CallSyncHelper", "Permission denial accessing call logs", e)
            AppLogger.log(context, "CallSyncHelper", "Permission denial accessing call logs", e)
        }
    }

    fun processCall(
        context: Context,
        number: String,
        type: Int,
        duration: Long,
        date: Long,
    ) {
        AppLogger.log(context, "CallSyncHelper", "processCall: number=$number, type=$type, duration=$duration, date=$date")
        val contactId = ContactHelper.getContactIdFromNumber(context, number) ?: return
        val contactName = ContactHelper.getContactName(context, contactId)

        val currentScore = PreferenceHelper.getContactScore(context, contactId)
        var newScore = currentScore
        var scoreChangeText = "No Change"
        var directionText = "Unknown"
        var typeText = "Unknown"

        when (type) {
            CallLog.Calls.MISSED_TYPE -> {
                directionText = "INCOMING"
                typeText = "MISSED"
                newScore += 1
                scoreChangeText = "+1"
            }
            CallLog.Calls.REJECTED_TYPE -> {
                directionText = "INCOMING"
                typeText = "REJECTED"
                newScore += 2
                scoreChangeText = "+2"
            }
            CallLog.Calls.INCOMING_TYPE -> {
                directionText = "INCOMING"
                if (duration > 0) {
                    typeText = "ANSWERED"
                    newScore = 0
                    scoreChangeText = "Reset to 0"
                } else {
                    typeText = "MISSED"
                    newScore += 1
                    scoreChangeText = "+1"
                }
            }
            CallLog.Calls.OUTGOING_TYPE -> {
                directionText = "OUTGOING"
                if (duration > 0) {
                    typeText = "ANSWERED"
                    newScore = 0
                    scoreChangeText = "Reset to 0"
                } else {
                    typeText = "UNANSWERED"
                    // Outgoing call unanswered does not change the score
                }
            }
        }

        if (newScore != currentScore || scoreChangeText != "No Change") {
            // Update preferences
            PreferenceHelper.setContactScore(context, contactId, newScore)

            // Update system contacts
            ContactHelper.updateContactRingtoneBasedOnScore(context, contactId, newScore)

            // Add call log entry to history
            val entry =
                CallLogEntry(
                    number = number,
                    name = contactName,
                    direction = directionText,
                    type = typeText,
                    timestamp = date,
                    scoreChange = scoreChangeText,
                )
            PreferenceHelper.addCallLogEntry(context, entry)
            Log.d("CallSyncHelper", "Processed call from $number: Score $currentScore -> $newScore ($scoreChangeText)")
            AppLogger.log(context, "CallSyncHelper", "Processed call from $number: Score $currentScore -> $newScore ($scoreChangeText)")
        }
    }
}
