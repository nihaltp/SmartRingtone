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

        contentUrisMock.`when`<Uri> { ContentUris.withAppendedId(Mockito.any(), Mockito.anyLong()) }.thenReturn(dummyUri)

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

    @Test
    fun testRestoreAllRingtonesToDefault() {
        val contactId = "123"
        val originalUri = "content://media/external/audio/media/99"

        // Mock SharedPreferences to return our backed up contact ringtone
        val allPrefs =
            mapOf(
                "orig_rt_$contactId" to originalUri,
            )
        Mockito.`when`(mockPrefs.all).thenReturn(allPrefs)
        Mockito.`when`(mockPrefs.getString("orig_rt_$contactId", null)).thenReturn(originalUri)

        // Mock content resolver input stream for isUriReadable check
        val dummyInputStream = mock<java.io.InputStream>()
        Mockito.`when`(mockContentResolver.openInputStream(any())).thenReturn(dummyInputStream)

        // Mock batch updates
        val mockResults = arrayOfNulls<android.content.ContentProviderResult>(1)
        Mockito.`when`(mockContentResolver.applyBatch(Mockito.anyString(), any())).thenReturn(mockResults)

        // Mock ContentProviderOperation static builder calls
        val mockBuilder = mock<android.content.ContentProviderOperation.Builder>()
        val mockOp = mock<android.content.ContentProviderOperation>()
        Mockito.`when`(mockBuilder.withValue(any(), any())).thenReturn(mockBuilder)
        Mockito.`when`(mockBuilder.build()).thenReturn(mockOp)

        val operationMock = Mockito.mockStatic(android.content.ContentProviderOperation::class.java)
        operationMock.`when`<android.content.ContentProviderOperation.Builder> {
            android.content.ContentProviderOperation.newUpdate(any())
        }.thenReturn(mockBuilder)

        try {
            ContactHelper.restoreAllRingtonesToDefault(mockContext)
        } finally {
            operationMock.close()
        }

        // Verify that the backup key was removed from SharedPreferences
        Mockito.verify(mockEditor).remove("orig_rt_$contactId")
        Mockito.verify(mockEditor).apply()
    }

    @Test
    fun testUpdateContactRingtoneBasedOnScoreWhenPaused() {
        val contactId = "123"

        // Mock app paused to true
        Mockito.`when`(mockPrefs.getBoolean("app_paused", false)).thenReturn(true)

        ContactHelper.updateContactRingtoneBasedOnScore(mockContext, contactId, 3)

        // Verify that it returned early and did not attempt to fetch original ringtone or set updates
        Mockito.verify(mockPrefs, Mockito.never()).getString(Mockito.startsWith("orig_rt_"), anyOrNull())
    }
}
