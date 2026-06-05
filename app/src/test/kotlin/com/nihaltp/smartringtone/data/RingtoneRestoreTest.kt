package com.nihaltp.smartringtone.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock

class RingtoneRestoreTest {
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockContentResolver: ContentResolver

    private lateinit var uriMock: MockedStatic<Uri>
    private lateinit var contentUrisMock: MockedStatic<ContentUris>
    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setUp() {
        mockContext = mock()
        mockPrefs = mock()
        mockEditor = mock()
        mockContentResolver = mock()

        uriMock = Mockito.mockStatic(Uri::class.java)
        contentUrisMock = Mockito.mockStatic(ContentUris::class.java)
        logMock = Mockito.mockStatic(Log::class.java)

        val dummyUri = mock<Uri>()
        uriMock.`when`<Uri> { Uri.withAppendedPath(any(), any()) }.thenReturn(dummyUri)
        uriMock.`when`<Uri> { Uri.parse(any()) }.thenReturn(dummyUri)
        uriMock.`when`<String> { Uri.encode(any()) }.thenReturn("encoded")

        contentUrisMock.`when`<Uri> { ContentUris.withAppendedId(any(), any()) }.thenReturn(dummyUri)

        Mockito.`when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        Mockito.`when`(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockPrefs.getBoolean(any(), any())).thenReturn(false)

        Mockito.`when`(mockEditor.putInt(any(), any())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putLong(any(), any())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.remove(any())).thenReturn(mockEditor)
    }

    @After
    fun tearDown() {
        uriMock.close()
        contentUrisMock.close()
        logMock.close()
    }

    @Test
    fun testDebugGetOriginalRingtone() {
        val contactId = "123"
        val originalUri = "content://media/external/audio/media/99"

        Mockito.`when`(mockPrefs.getString(any(), anyOrNull())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            println("DEBUG mockPrefs.getString called with key: $key")
            if (key == "orig_rt_$contactId") originalUri else null
        }

        val original = PreferenceHelper.getOriginalRingtone(mockContext, contactId)
        println("DEBUG original retrieved: $original")
        assertEquals(originalUri, original)
    }
}
