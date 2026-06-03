package com.nihaltp.smartringtone.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

object ContactHelper {

    fun getContacts(context: Context): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        try {
            val contentResolver = context.contentResolver
            val ringtones = PreferenceHelper.getRingtones(context)

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                val seenIds = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val contactId = it.getString(idCol) ?: continue
                    if (seenIds.contains(contactId)) continue
                    seenIds.add(contactId)

                    val name = it.getString(nameCol) ?: "Unknown"
                    val phone = it.getString(numCol) ?: ""
                    val photoUri = it.getString(photoCol)

                    val customRingtone = getContactCurrentRingtoneUri(context, contactId)
                    val score = PreferenceHelper.getContactScore(context, contactId)

                    val mappedRingtoneName = if (score > 0 && ringtones.isNotEmpty()) {
                        val index = (score - 1).coerceAtMost(ringtones.size - 1)
                        ringtones[index].name
                    } else {
                        val original = PreferenceHelper.getOriginalRingtone(context, contactId)
                        if (original != null && original != PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER) {
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
                            mappedRingtoneName = mappedRingtoneName
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("ContactHelper", "Permission denial reading contacts", e)
        }
        return contactsList
    }

    fun getContactCurrentRingtoneUri(context: Context, contactId: String): String? {
        return try {
            val contentResolver = context.contentResolver
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            var customRingtone: String? = null

            val contactCursor = contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts.CUSTOM_RINGTONE),
                null,
                null,
                null
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

    fun getContactIdFromNumber(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isEmpty()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
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

    fun getContactName(context: Context, contactId: String): String {
        return try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null
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

    fun setContactRingtone(context: Context, contactId: String, ringtoneUriString: String?): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUriString)
            }
            val rows = contentResolver.update(contactUri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e("ContactHelper", "Failed to update ringtone for contact $contactId", e)
            false
        }
    }

    fun updateContactRingtoneBasedOnScore(context: Context, contactId: String, newScore: Int) {
        val ringtones = PreferenceHelper.getRingtones(context)
        
        if (newScore == 0) {
            // Restore original ringtone
            val original = PreferenceHelper.getOriginalRingtone(context, contactId)
            if (original != null) {
                val uriToSet = if (original == PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER) null else original
                setContactRingtone(context, contactId, uriToSet)
                PreferenceHelper.setOriginalRingtone(context, contactId, null) // clear original backup
            } else {
                // If there's no backup, we just set to null (default)
                setContactRingtone(context, contactId, null)
            }
        } else {
            // Check if we need to backup the original ringtone first (only if no backup exists yet)
            val currentBackup = PreferenceHelper.getOriginalRingtone(context, contactId)
            if (currentBackup == null) {
                val currentRingtone = getContactCurrentRingtoneUri(context, contactId)
                val backupVal = currentRingtone ?: PreferenceHelper.ORIGINAL_RINGTONE_DEFAULT_PLACEHOLDER
                PreferenceHelper.setOriginalRingtone(context, contactId, backupVal)
            }

            // Set the custom ringtone based on score
            if (ringtones.isNotEmpty()) {
                val index = (newScore - 1).coerceAtMost(ringtones.size - 1)
                val ringtoneUri = ringtones[index].uri
                setContactRingtone(context, contactId, ringtoneUri)
            }
        }
    }
}
