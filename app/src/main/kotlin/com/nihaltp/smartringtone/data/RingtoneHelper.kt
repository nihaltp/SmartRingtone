package com.nihaltp.smartringtone.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.InputStream
import java.io.OutputStream

object RingtoneHelper {
    fun persistRingtone(
        context: Context,
        sourceUri: Uri,
    ): Pair<String, String>? {
        val contentResolver = context.contentResolver
        var fileName = "CustomRingtone.mp3"
        var mimeType = "audio/mp3"

        // Get file name and details from picker Uri
        contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: "CustomRingtone.mp3"
                }
            }
        }

        if (fileName.endsWith(".wav", ignoreCase = true)) {
            mimeType = "audio/wav"
        } else if (fileName.endsWith(".ogg", ignoreCase = true)) {
            mimeType = "audio/ogg"
        } else if (fileName.endsWith(".flac", ignoreCase = true)) {
            mimeType = "audio/flac"
        } else if (fileName.endsWith(".m4a", ignoreCase = true)) {
            mimeType = "audio/mp4"
        }

        // Deduplicate: check if a ringtone with this name is already in the MediaStore
        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
            )
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.IS_RINGTONE} = 1"
        val selectionArgs = arrayOf(fileName)

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    if (idCol >= 0) {
                        val mediaId = cursor.getLong(idCol)
                        val existingUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId.toString())
                        var isReadable = false
                        try {
                            context.contentResolver.openInputStream(existingUri)?.use {
                                isReadable = true
                            }
                        } catch (e: Exception) {
                            Log.w("RingtoneHelper", "Existing ringtone URI found but not readable: $existingUri", e)
                        }
                        if (isReadable) {
                            Log.d("RingtoneHelper", "Found existing readable ringtone URI: $existingUri")
                            return Pair(fileName, existingUri.toString())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RingtoneHelper", "Failed checking for existing ringtone", e)
        }

        // Insert new entry in MediaStore
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

        return try {
            val newUri =
                contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null

            // Copy data from sourceUri to newUri
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = contentResolver.openInputStream(sourceUri)
                outputStream = contentResolver.openOutputStream(newUri)
                if (inputStream != null && outputStream != null) {
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            } finally {
                inputStream?.close()
                outputStream?.close()
            }

            Pair(fileName, newUri.toString())
        } catch (e: Exception) {
            Log.e("RingtoneHelper", "Failed to add ringtone to MediaStore", e)
            null
        }
    }

    fun addRingtoneFromUri(
        context: Context,
        sourceUri: Uri,
    ): Ringtone? {
        val persisted = persistRingtone(context, sourceUri) ?: return null
        val ringtones = PreferenceHelper.getRingtones(context)
        val nextId = (ringtones.maxOfOrNull { it.id } ?: 0) + 1
        return Ringtone(id = nextId, name = persisted.first, uri = persisted.second)
    }
}
