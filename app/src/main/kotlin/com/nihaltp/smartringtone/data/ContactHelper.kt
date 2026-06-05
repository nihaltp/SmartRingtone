package com.nihaltp.smartringtone.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.nihaltp.smartringtone.NotificationHelper

object ContactHelper {
    fun getContacts(context: Context): List<Contact> {
        if (PreferenceHelper.isScreenshotMode(context)) {
            val ringtones = PreferenceHelper.getRingtones(context)
            val allScores = PreferenceHelper.getAllScores(context)

            fun getMappedName(score: Int): String {
                return if (score > 0 && ringtones.isNotEmpty()) {
                    val index = (score - 1).coerceAtMost(ringtones.size - 1)
                    ringtones[index].name
                } else {
                    "System Default"
                }
            }
            return listOf(
                Contact(
                    id = "mock_1",
                    name = "Alice Vance",
                    phone = "+1 (123) 456-1428",
                    photoUri = null,
                    currentRingtone = null,
                    score = allScores["mock_1"] ?: 0,
                    mappedRingtoneName = getMappedName(allScores["mock_1"] ?: 0),
                ),
                Contact(
                    id = "mock_2",
                    name = "Bob Miller",
                    phone = "+1 (123) 456-9284",
                    photoUri = null,
                    currentRingtone = null,
                    score = allScores["mock_2"] ?: 2,
                    mappedRingtoneName = getMappedName(allScores["mock_2"] ?: 2),
                ),
                Contact(
                    id = "mock_3",
                    name = "Charlie Ross",
                    phone = "+1 (123) 456-3841",
                    photoUri = null,
                    currentRingtone = null,
                    score = allScores["mock_3"] ?: 4,
                    mappedRingtoneName = getMappedName(allScores["mock_3"] ?: 4),
                ),
            )
        }

        val contactsList = mutableListOf<Contact>()
        try {
            val contentResolver = context.contentResolver
            val ringtones = PreferenceHelper.getRingtones(context)
            val allScores = PreferenceHelper.getAllScores(context)
            val allOriginals = PreferenceHelper.getAllOriginalRingtones(context)

            val cursor =
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                        ContactsContract.Contacts.CUSTOM_RINGTONE,
                    ),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
                )

            cursor?.use {
                val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val ringtoneCol = it.getColumnIndex(ContactsContract.Contacts.CUSTOM_RINGTONE)

                val seenIds = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val contactId = it.getString(idCol) ?: continue
                    if (seenIds.contains(contactId)) continue
                    seenIds.add(contactId)

                    val name = it.getString(nameCol) ?: "Unknown"
                    val phone = it.getString(numCol) ?: ""
                    val photoUri = it.getString(photoCol)

                    val customRingtone = if (ringtoneCol >= 0) it.getString(ringtoneCol) else null
                    val score = allScores[contactId] ?: 0

                    val mappedRingtoneName =
                        if (score > 0 && ringtones.isNotEmpty()) {
                            val index = (score - 1).coerceAtMost(ringtones.size - 1)
                            ringtones[index].name
                        } else {
                            val original = allOriginals[contactId]
                            if (!original.isNullOrEmpty() && original != PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER) {
                                "Original (Custom)"
                            } else {
                                "System Default"
                            }
                        }

                    contactsList.add(
                        Contact(
                            id = contactId,
                            name = name,
                            phone = phone,
                            photoUri = photoUri,
                            currentRingtone = customRingtone,
                            score = score,
                            mappedRingtoneName = mappedRingtoneName,
                        ),
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("ContactHelper", "Permission denial reading contacts", e)
        }
        return contactsList
    }

    fun getContactCurrentRingtoneUri(
        context: Context,
        contactId: String,
    ): String? {
        return try {
            val contentResolver = context.contentResolver
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            var customRingtone: String? = null

            val contactCursor =
                contentResolver.query(
                    contactUri,
                    arrayOf(ContactsContract.Contacts.CUSTOM_RINGTONE),
                    null,
                    null,
                    null,
                )
            contactCursor?.use { cc ->
                if (cc.moveToFirst()) {
                    val ringtoneCol = cc.getColumnIndex(ContactsContract.Contacts.CUSTOM_RINGTONE)
                    if (ringtoneCol >= 0) {
                        customRingtone = cc.getString(ringtoneCol)
                    }
                }
            }
            customRingtone
        } catch (e: SecurityException) {
            Log.e("ContactHelper", "Permission denial reading custom ringtone", e)
            null
        }
    }

    fun getContactIdFromNumber(
        context: Context,
        phoneNumber: String,
    ): String? {
        if (phoneNumber.isEmpty()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor =
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup._ID),
                    null,
                    null,
                    null,
                )
            cursor?.use {
                if (it.moveToFirst()) {
                    val idCol = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idCol >= 0) {
                        return it.getString(idCol)
                    }
                }
            }
            null
        } catch (e: SecurityException) {
            Log.e("ContactHelper", "Permission denial lookup phone number", e)
            null
        }
    }

    fun getContactName(
        context: Context,
        contactId: String,
    ): String {
        return try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val cursor =
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameCol = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameCol >= 0) {
                        return it.getString(nameCol) ?: "Unknown"
                    }
                }
            }
            "Unknown"
        } catch (e: SecurityException) {
            Log.e("ContactHelper", "Permission denial reading contact name", e)
            "Unknown"
        }
    }

    fun setContactRingtone(
        context: Context,
        contactId: String,
        ringtoneUriString: String?,
    ): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val values =
                ContentValues().apply {
                    put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUriString)
                }
            val rows = contentResolver.update(contactUri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e("ContactHelper", "Failed to update ringtone for contact $contactId", e)
            false
        }
    }

    fun isUriReadable(
        context: Context,
        uriString: String?,
    ): Boolean {
        if (uriString.isNullOrEmpty()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateContactRingtoneBasedOnScore(
        context: Context,
        contactId: String,
        newScore: Int,
        ringtones: List<Ringtone>? = null,
    ) {
        if (PreferenceHelper.isScreenshotMode(context)) return
        val activeRingtones = ringtones ?: PreferenceHelper.getRingtones(context)

        if (newScore == 0) {
            // Restore original ringtone
            val original = PreferenceHelper.getOriginalRingtone(context, contactId)
            val fallbackUri = PreferenceHelper.getFallbackRingtoneUri(context)

            val uriToSet: String?
            if (original != null) {
                if (original == PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER) {
                    uriToSet = fallbackUri
                } else {
                    if (isUriReadable(context, original)) {
                        uriToSet = original
                    } else {
                        uriToSet = fallbackUri
                    }
                }
            } else {
                uriToSet = fallbackUri
            }

            val success = setContactRingtone(context, contactId, uriToSet)
            if (!success && uriToSet != fallbackUri && fallbackUri != null) {
                setContactRingtone(context, contactId, fallbackUri)
            }
            PreferenceHelper.setOriginalRingtone(context, contactId, null) // clear original backup
        } else {
            // Check if we need to backup the original ringtone first (only if no backup exists yet)
            val currentBackup = PreferenceHelper.getOriginalRingtone(context, contactId)
            if (currentBackup == null) {
                val currentRingtone = getContactCurrentRingtoneUri(context, contactId)
                val backupVal = currentRingtone ?: PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER
                PreferenceHelper.setOriginalRingtone(context, contactId, backupVal)
            }

            // Set the custom ringtone based on score
            if (activeRingtones.isNotEmpty()) {
                val index = (newScore - 1).coerceAtMost(activeRingtones.size - 1)
                val ringtoneUri = activeRingtones[index].uri
                setContactRingtone(context, contactId, ringtoneUri)
            }
        }
    }

    fun updateContactsRingtones(
        context: Context,
        updates: List<Pair<String, String?>>,
    ): Boolean {
        if (updates.isEmpty()) return true
        val ops = ArrayList<ContentProviderOperation>()
        for ((contactId, ringtoneUri) in updates) {
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val op =
                ContentProviderOperation.newUpdate(contactUri)
                    .withValue(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri)
                    .build()
            ops.add(op)
        }
        return try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            Log.e("ContactHelper", "Failed to batch update ringtones", e)
            false
        }
    }

    fun updateContactsRingtonesBasedOnScores(
        context: Context,
        contacts: List<Contact>,
        ringtones: List<Ringtone>,
    ) {
        if (PreferenceHelper.isScreenshotMode(context)) return
        val prefs = context.getSharedPreferences("RingtoneChangerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var editorModified = false

        val updates = mutableListOf<Pair<String, String?>>()
        val scoredContacts = contacts.filter { it.score > 0 }

        if (scoredContacts.isEmpty()) return

        for (c in scoredContacts) {
            if (ringtones.isNotEmpty()) {
                val index = (c.score - 1).coerceAtMost(ringtones.size - 1)
                val ringtoneUri = ringtones[index].uri

                // Backup original if not backed up yet
                val origKey = "orig_rt_${c.id}"
                val currentBackup = prefs.getString(origKey, null)
                if (currentBackup == null) {
                    val backupVal = c.currentRingtone ?: PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER
                    editor.putString(origKey, backupVal)
                    editorModified = true
                }
                updates.add(c.id to ringtoneUri)
            }
        }
        if (editorModified) {
            editor.apply()
        }

        if (updates.isNotEmpty()) {
            val total = updates.size
            val chunkSize = 50
            for (i in 0 until total step chunkSize) {
                val chunk = updates.subList(i, minOf(i + chunkSize, total))
                updateContactsRingtones(context, chunk)
                NotificationHelper.showProgressNotification(context, minOf(i + chunkSize, total), total)
            }
            NotificationHelper.dismissNotification(context)
        }
    }

    fun resetAllScores(
        context: Context,
        contacts: List<Contact>,
    ) {
        if (PreferenceHelper.isScreenshotMode(context)) {
            val prefs = context.getSharedPreferences("RingtoneChangerPrefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            for (c in contacts) {
                editor.remove("score_${c.id}")
            }
            editor.apply()
            return
        }
        val prefs = context.getSharedPreferences("RingtoneChangerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val updates = mutableListOf<Pair<String, String?>>()
        val scoredContacts = contacts.filter { it.score > 0 }

        if (scoredContacts.isEmpty()) return

        val fallbackUri = PreferenceHelper.getFallbackRingtoneUri(context)

        for (c in scoredContacts) {
            editor.remove("score_${c.id}")
            val origKey = "orig_rt_${c.id}"
            val original = prefs.getString(origKey, null)
            if (original != null) {
                val uriToSet: String?
                if (original == PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER) {
                    uriToSet = fallbackUri
                } else {
                    if (isUriReadable(context, original)) {
                        uriToSet = original
                    } else {
                        uriToSet = fallbackUri
                    }
                }
                updates.add(c.id to uriToSet)
                editor.remove(origKey)
            } else {
                updates.add(c.id to fallbackUri)
            }
        }
        editor.apply()

        if (updates.isNotEmpty()) {
            val total = updates.size
            val chunkSize = 50
            for (i in 0 until total step chunkSize) {
                val chunk = updates.subList(i, minOf(i + chunkSize, total))
                updateContactsRingtones(context, chunk)
                NotificationHelper.showProgressNotification(context, minOf(i + chunkSize, total), total)
            }
            NotificationHelper.dismissNotification(context)
        }
    }
}
