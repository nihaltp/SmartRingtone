package com.nihaltp.smartringtone

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.util.Log
import com.nihaltp.smartringtone.data.CallSyncHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class CallSyncUnitTest {
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

        // Mock Android framework static methods
        uriMock = Mockito.mockStatic(Uri::class.java)
        contentUrisMock = Mockito.mockStatic(ContentUris::class.java)
        logMock = Mockito.mockStatic(Log::class.java)

        val dummyUri = mock<Uri>()
        uriMock.`when`<Uri> { Uri.withAppendedPath(Mockito.any(), Mockito.anyString()) }.thenReturn(dummyUri)
        uriMock.`when`<Uri> { Uri.parse(Mockito.anyString()) }.thenReturn(dummyUri)
        uriMock.`when`<String> { Uri.encode(Mockito.anyString()) }.thenReturn("encoded")

        contentUrisMock.`when`<Uri> { ContentUris.withAppendedId(Mockito.any(), Mockito.anyLong()) }.thenReturn(dummyUri)

        Mockito.`when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        Mockito.`when`(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockPrefs.getBoolean(any(), any())).thenReturn(false)
        Mockito.`when`(mockPrefs.all).thenReturn(emptyMap<String, Any>())

        // Stub editor chains
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
    fun testProcessCallRejectedIncrementsScoreByTwo() {
        val testNumber = "+15550199"
        val contactId = "123"

        // Mock phone lookup query cursor (1st query)
        val mockLookupCursor = mock<Cursor>()
        Mockito.`when`(mockLookupCursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(mockLookupCursor.getColumnIndex(Mockito.anyString())).thenReturn(0)
        Mockito.`when`(mockLookupCursor.getString(0)).thenReturn(contactId)

        // Mock name lookup query cursor (2nd query)
        val mockNameCursor = mock<Cursor>()
        Mockito.`when`(mockNameCursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(mockNameCursor.getColumnIndex(Mockito.anyString())).thenReturn(0)
        Mockito.`when`(mockNameCursor.getString(0)).thenReturn("Test Contact")

        // Mock ContentResolver queries sequentially
        Mockito.`when`(
            mockContentResolver.query(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
            ),
        ).thenReturn(mockLookupCursor, mockNameCursor)

        // Mock getContactScore
        Mockito.`when`(mockPrefs.getInt(eq("score_$contactId"), any())).thenReturn(0)

        // Mock ringtone list (empty)
        Mockito.`when`(mockPrefs.getString(eq("ringtones"), any())).thenReturn("[]")

        // Mock contact update ringtone (returns 1 row updated)
        Mockito.`when`(mockContentResolver.update(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(1)

        // Call method under test
        CallSyncHelper.processCall(
            context = mockContext,
            number = testNumber,
            type = CallLog.Calls.REJECTED_TYPE,
            duration = 0L,
            date = 1000L,
        )

        // Verify score was set to 2 in SharedPreferences
        Mockito.verify(mockEditor).putInt(eq("score_$contactId"), eq(2))
    }

    @Test
    fun testSyncCallLogsWatermarkDoesNotAdvanceIfNoLogs() {
        val initialSyncTime = 5000L
        Mockito.`when`(mockPrefs.getLong(eq("last_sync_time"), any())).thenReturn(initialSyncTime)

        // Return empty cursor for CallLog
        val mockCallLogCursor = mock<Cursor>()
        Mockito.`when`(mockCallLogCursor.moveToNext()).thenReturn(false)
        Mockito.`when`(
            mockContentResolver.query(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
            ),
        ).thenReturn(mockCallLogCursor)

        CallSyncHelper.syncCallLogs(mockContext)

        // Verify last_sync_time was NOT updated
        Mockito.verify(mockEditor, Mockito.never()).putLong(eq("last_sync_time"), any())
    }

    @Test
    fun testSyncCallLogsWatermarkAdvancesToNewestLog() {
        val initialSyncTime = 5000L
        Mockito.`when`(mockPrefs.getLong(eq("last_sync_time"), any())).thenReturn(initialSyncTime)

        // Return one call log entry newer than initialSyncTime
        val mockCallLogCursor = mock<Cursor>()
        Mockito.`when`(mockCallLogCursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.NUMBER)).thenReturn(0)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.TYPE)).thenReturn(1)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DURATION)).thenReturn(2)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DATE)).thenReturn(3)

        Mockito.`when`(mockCallLogCursor.getString(0)).thenReturn("+15550199")
        Mockito.`when`(mockCallLogCursor.getInt(1)).thenReturn(CallLog.Calls.REJECTED_TYPE)
        Mockito.`when`(mockCallLogCursor.getLong(2)).thenReturn(0L)
        Mockito.`when`(mockCallLogCursor.getLong(3)).thenReturn(6000L) // 6000 > 5000

        // 1st query: CallLog
        // 2nd query: Phone lookup (returns null, meaning no contact matches, to skip details and verify watermark)
        Mockito.`when`(
            mockContentResolver.query(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
            ),
        ).thenReturn(mockCallLogCursor, null)

        CallSyncHelper.syncCallLogs(mockContext)

        // Verify last_sync_time was updated to 6000L
        Mockito.verify(mockEditor).putLong(eq("last_sync_time"), eq(6000L))
        Mockito.verify(mockEditor).apply()
    }

    @Test
    fun testSyncCallLogsFirstInstallScansWholeLog() {
        // First install: last_sync_time is not set, so getLong returns default (0L)
        Mockito.`when`(mockPrefs.getLong(eq("last_sync_time"), any())).thenReturn(0L)

        // Mock 2 call log entries
        val mockCallLogCursor = mock<Cursor>()
        Mockito.`when`(mockCallLogCursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.NUMBER)).thenReturn(0)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.TYPE)).thenReturn(1)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DURATION)).thenReturn(2)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DATE)).thenReturn(3)

        Mockito.`when`(mockCallLogCursor.getString(0)).thenReturn("+15550199")
        Mockito.`when`(mockCallLogCursor.getInt(1)).thenReturn(CallLog.Calls.REJECTED_TYPE, CallLog.Calls.MISSED_TYPE)
        Mockito.`when`(mockCallLogCursor.getLong(2)).thenReturn(0L, 0L)
        Mockito.`when`(mockCallLogCursor.getLong(3)).thenReturn(1000L, 2000L)

        // Mock ContactHelper lookups
        val contactId = "123"
        val mockLookupCursor = mock<Cursor>()
        Mockito.`when`(mockLookupCursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(mockLookupCursor.getColumnIndex(Mockito.anyString())).thenReturn(0)
        Mockito.`when`(mockLookupCursor.getString(0)).thenReturn(contactId)

        val mockNameCursor = mock<Cursor>()
        Mockito.`when`(mockNameCursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(mockNameCursor.getColumnIndex(Mockito.anyString())).thenReturn(0)
        Mockito.`when`(mockNameCursor.getString(0)).thenReturn("Test Contact")

        // Mock queries sequentially
        // 1. CallLog query
        // 2. Contact ID lookup (for number +15550199)
        // 3. Contact name lookup (for contactId 123)
        Mockito.`when`(
            mockContentResolver.query(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
            ),
        ).thenReturn(mockCallLogCursor, mockLookupCursor, mockNameCursor)

        // Mock preferences score lookup to return 0 initially
        Mockito.`when`(mockPrefs.getInt(eq("score_$contactId"), any())).thenReturn(0)
        Mockito.`when`(mockPrefs.all).thenReturn(emptyMap<String, Any>())

        // Run sync
        CallSyncHelper.syncCallLogs(mockContext)

        // Verify last_sync_time was updated to 2000L
        Mockito.verify(mockEditor).putLong(eq("last_sync_time"), eq(2000L))
        // Verify final score (2 from rejected + 1 from missed = 3) was saved
        Mockito.verify(mockEditor).putInt(eq("score_$contactId"), eq(3))
        // Verify batch addCallLogEntries was called (which puts the history string into editor)
        Mockito.verify(mockEditor).putString(eq("call_logs_history"), Mockito.anyString())
        Mockito.verify(mockEditor, Mockito.atLeastOnce()).apply()
    }
}
