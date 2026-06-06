package com.nihaltp.smartringtone.data

import android.content.Context
import android.provider.CallLog
import android.util.Log
import com.nihaltp.smartringtone.R

object CallSyncHelper {
    fun syncCallLogs(context: Context) {
        synchronized(syncLock) {
            if (PreferenceHelper.isScreenshotMode(context)) return
            val lastSyncTime = PreferenceHelper.getLastSyncTime(context)
            AppLogger.log(context, "CallSyncHelper", "syncCallLogs starting from lastSyncTime=$lastSyncTime")
            val contentResolver = context.contentResolver

            // Query all calls since lastSyncTime, sorted DESCENDING (newest first)
            val projection =
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE,
                )
            val selection = "${CallLog.Calls.DATE} > ?"
            val selectionArgs = arrayOf(lastSyncTime.toString())
            val sortOrder = "${CallLog.Calls.DATE} DESC"

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

                    // Cache for looking up contact information during this sync run
                    val numberToContactId = mutableMapOf<String, String?>()
                    val contactIdToName = mutableMapOf<String, String>()

                    // Keep track of scores in memory during the sync
                    val contactScores = PreferenceHelper.getAllScores(context).toMutableMap()
                    val initialScores = contactScores.toMap()

                    val newEntries = mutableListOf<CallLogEntry>()
                    val history = PreferenceHelper.getCallLogsHistory(context).toMutableList()
                    var historyModified = false

                    // Set of contacts who have encountered a reset (answered call) in this sync run
                    val resetContacts = mutableSetOf<String>()

                    // Map of accumulated score contributions before encountering a reset call
                    val windowScores = mutableMapOf<String, Int>()

                    val totalLogs = it.count
                    var processedLogs = 0

                    while (it.moveToNext()) {
                        processedLogs++
                        if (totalLogs > 0) {
                            com.nihaltp.smartringtone.NotificationHelper.showNotification(
                                context,
                                context.getString(R.string.sync_title),
                                context.getString(R.string.sync_scanning_logs_progress, processedLogs, totalLogs),
                                progress = processedLogs,
                                maxProgress = totalLogs,
                            )
                        }
                        val number = it.getString(numCol) ?: ""
                        val type = it.getInt(typeCol)
                        val duration = it.getLong(durCol)
                        val date = it.getLong(dateCol)

                        if (date > newLastSyncTime) {
                            newLastSyncTime = date
                        }

                        // Look up contact ID
                        val contactId =
                            numberToContactId.getOrPut(number) {
                                ContactHelper.getContactIdFromNumber(context, number)
                            }
                        if (contactId == null) {
                            AppLogger.log(context, "CallSyncHelper", "No contact found for number=$number")
                            continue
                        }

                        // Early exit if this contact has already encountered a reset call
                        if (resetContacts.contains(contactId)) {
                            continue
                        }

                        val contactName =
                            contactIdToName.getOrPut(contactId) {
                                ContactHelper.getContactName(context, contactId)
                            }

                        var scoreChangeText = "No Change"
                        var directionText = "Unknown"
                        var typeText = "Unknown"
                        var isReset = false
                        var scoreDelta = 0

                        when (type) {
                            CallLog.Calls.MISSED_TYPE -> {
                                directionText = "INCOMING"
                                typeText = "MISSED"
                                scoreChangeText = "+1"
                                scoreDelta = 1
                            }
                            CallLog.Calls.REJECTED_TYPE -> {
                                directionText = "INCOMING"
                                typeText = "REJECTED"
                                scoreChangeText = "+2"
                                scoreDelta = 2
                            }
                            CallLog.Calls.INCOMING_TYPE -> {
                                directionText = "INCOMING"
                                if (duration > 0) {
                                    typeText = "ANSWERED"
                                    scoreChangeText = "Reset to 0"
                                    isReset = true
                                } else {
                                    typeText = "MISSED"
                                    scoreChangeText = "+1"
                                    scoreDelta = 1
                                }
                            }
                            CallLog.Calls.OUTGOING_TYPE -> {
                                directionText = "OUTGOING"
                                if (duration > 0) {
                                    typeText = "ANSWERED"
                                    scoreChangeText = "Reset to 0"
                                    isReset = true
                                } else {
                                    typeText = "UNANSWERED"
                                }
                            }
                        }

                        if (isReset) {
                            resetContacts.add(contactId)
                            // Final score is just what we accumulated since the reset call (chronologically after it)
                            val finalScore = windowScores[contactId] ?: 0
                            contactScores[contactId] = finalScore

                            // Remove any older history entries for this contact from the existing history
                            val removedFromHistory =
                                history.removeAll { entry ->
                                    val entryContactId = ContactHelper.getContactIdFromNumber(context, entry.number)
                                    val matchesId = entryContactId == contactId
                                    val matchesNumber = entry.number == number
                                    val matchesName = contactName.isNotEmpty() && entry.name == contactName
                                    matchesId || matchesNumber || matchesName
                                }
                            if (removedFromHistory) {
                                historyModified = true
                            }

                            val entry =
                                CallLogEntry(
                                    number = number,
                                    name = contactName,
                                    direction = directionText,
                                    type = typeText,
                                    timestamp = date,
                                    scoreChange = scoreChangeText,
                                )
                            newEntries.add(entry)
                            Log.d(
                                "CallSyncHelper",
                                "In-memory processed reset call from $number: Score set to $finalScore",
                            )
                        } else if (scoreChangeText != "No Change") {
                            // Accumulate window score for non-reset calls
                            val currentWin = windowScores[contactId] ?: 0
                            windowScores[contactId] = currentWin + scoreDelta

                            val entry =
                                CallLogEntry(
                                    number = number,
                                    name = contactName,
                                    direction = directionText,
                                    type = typeText,
                                    timestamp = date,
                                    scoreChange = scoreChangeText,
                                )
                            newEntries.add(entry)
                            Log.d(
                                "CallSyncHelper",
                                "In-memory processed call from $number: windowScore delta $scoreChangeText",
                            )
                        }
                    }

                    // Apply accumulated scores for contacts who did NOT encounter a reset call in the window
                    for ((contactId, winScore) in windowScores) {
                        if (!resetContacts.contains(contactId)) {
                            val initial = initialScores[contactId] ?: 0
                            contactScores[contactId] = initial + winScore
                            Log.d(
                                "CallSyncHelper",
                                "Applied window score for contact $contactId: $initial -> ${initial + winScore}",
                            )
                        }
                    }

                    // Write final scores to SharedPreferences and update system contact ringtones
                    val changedScores =
                        contactScores.filter { (contactId, finalScore) ->
                            finalScore != (initialScores[contactId] ?: 0)
                        }.toList()

                    val totalChanged = changedScores.size
                    var processedCount = 0

                    for ((contactId, finalScore) in changedScores) {
                        PreferenceHelper.setContactScore(context, contactId, finalScore)
                        ContactHelper.updateContactRingtoneBasedOnScore(context, contactId, finalScore)
                        processedCount++
                        if (totalChanged > 0) {
                            com.nihaltp.smartringtone.NotificationHelper.showNotification(
                                context,
                                context.getString(R.string.sync_title),
                                context.getString(R.string.sync_updating_ringtones, processedCount, totalChanged),
                                progress = processedCount,
                                maxProgress = totalChanged,
                            )
                        }
                        Log.d("CallSyncHelper", "Applied final score for contact $contactId: $finalScore")
                        AppLogger.log(context, "CallSyncHelper", "Applied final score for contact $contactId: $finalScore")
                    }
                    if (totalChanged > 0) {
                        com.nihaltp.smartringtone.NotificationHelper.dismissNotification(context)
                    }

                    // Save all new call log entries to history in a single write
                    if (newEntries.isNotEmpty() || historyModified) {
                        history.addAll(0, newEntries)
                        while (history.size > 50) {
                            history.removeAt(history.size - 1)
                        }
                        PreferenceHelper.saveCallLogsHistory(context, history)
                    }
                }

                // Only update the last sync time if we actually processed call logs with a newer timestamp
                if (newLastSyncTime > lastSyncTime) {
                    PreferenceHelper.setLastSyncTime(context, newLastSyncTime)
                }
            } catch (e: SecurityException) {
                Log.e("CallSyncHelper", "Permission denial accessing call logs", e)
                AppLogger.log(context, "CallSyncHelper", "Permission denial accessing call logs", e)
            }
        }
    }

    fun processCall(
        context: Context,
        number: String,
        type: Int,
        duration: Long,
        date: Long,
    ) {
        synchronized(syncLock) {
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
                if (scoreChangeText == "Reset to 0") {
                    PreferenceHelper.clearCallLogsForContact(context, contactId)
                }
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
}
