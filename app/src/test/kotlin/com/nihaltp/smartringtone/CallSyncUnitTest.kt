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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyVararg
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
        Mockito.`when`(mockContext.getString(any())).thenReturn("Dummy String")
        Mockito.`when`(mockContext.getString(any(), anyVararg())).thenReturn("Dummy String")

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

    @Test
    fun testSyncCallLogsReverseScanStopsAtReset() {
        // Mock 3 call log entries:
        // 1st (newest): Missed (type = MISSED_TYPE, date = 3000L)
        // 2nd: Answered (type = INCOMING_TYPE, duration = 10, date = 2000L) -> Reset
        // 3rd: Rejected (type = REJECTED_TYPE, date = 1000L) -> Should be ignored
        Mockito.`when`(mockPrefs.getLong(eq("last_sync_time"), any())).thenReturn(0L)

        val mockCallLogCursor = mock<Cursor>()
        Mockito.`when`(mockCallLogCursor.moveToNext()).thenReturn(true, true, true, false)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.NUMBER)).thenReturn(0)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.TYPE)).thenReturn(1)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DURATION)).thenReturn(2)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DATE)).thenReturn(3)

        Mockito.`when`(mockCallLogCursor.getString(0)).thenReturn("+15550199")
        // Return types: Missed (1st), Answered (2nd), Rejected (3rd)
        Mockito.`when`(mockCallLogCursor.getInt(1)).thenReturn(
            CallLog.Calls.MISSED_TYPE,
            CallLog.Calls.INCOMING_TYPE,
            CallLog.Calls.REJECTED_TYPE,
        )
        // Return durations: 0 (1st), 10 (2nd), 0 (3rd)
        Mockito.`when`(mockCallLogCursor.getLong(2)).thenReturn(0L, 10L, 0L)
        // Return dates: 3000L (1st), 2000L (2nd), 1000L (3rd)
        Mockito.`when`(mockCallLogCursor.getLong(3)).thenReturn(3000L, 2000L, 1000L)

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

        Mockito.`when`(mockPrefs.getInt(eq("score_$contactId"), any())).thenReturn(0)
        Mockito.`when`(mockPrefs.all).thenReturn(emptyMap<String, Any>())

        // Run sync
        CallSyncHelper.syncCallLogs(mockContext)

        // Verify last_sync_time was updated to 3000L
        Mockito.verify(mockEditor).putLong(eq("last_sync_time"), eq(3000L))
        // Verify final score is 1 (the missed call that happened AFTER the reset call)
        // It should NOT be 3 (since the rejected call happened BEFORE the reset call and is ignored)
        Mockito.verify(mockEditor).putInt(eq("score_$contactId"), eq(1))
        Mockito.verify(mockEditor, Mockito.atLeastOnce()).apply()
    }

    @Test
    fun testSyncCallLogsReverseScanLongListWithResets() {
        Mockito.`when`(mockPrefs.getLong(eq("last_sync_time"), any())).thenReturn(0L)

        val mockCallLogCursor = mock<Cursor>()
        var currentRow = -1
        Mockito.`when`(mockCallLogCursor.moveToNext()).thenAnswer {
            currentRow++
            currentRow < 100
        }
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.NUMBER)).thenReturn(0)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.TYPE)).thenReturn(1)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DURATION)).thenReturn(2)
        Mockito.`when`(mockCallLogCursor.getColumnIndex(CallLog.Calls.DATE)).thenReturn(3)

        // Generate 100 logs
        val numbers =
            Array(100) { i ->
                when (i % 3) {
                    0 -> "+15550001"
                    1 -> "+15550002"
                    else -> "+15550003"
                }
            }
        val types =
            Array(100) { i ->
                when (i) {
                    15 -> CallLog.Calls.INCOMING_TYPE
                    45 -> CallLog.Calls.INCOMING_TYPE
                    2 -> CallLog.Calls.INCOMING_TYPE
                    else -> if (i % 2 == 0) CallLog.Calls.MISSED_TYPE else CallLog.Calls.REJECTED_TYPE
                }
            }
        val durations =
            Array(100) { i ->
                if (i == 15 || i == 45 || i == 2) 10L else 0L
            }
        val dates =
            Array(100) { i ->
                100000L + (100 - i) * 1000L
            }

        Mockito.`when`(mockCallLogCursor.getString(0)).thenAnswer { numbers[currentRow] }
        Mockito.`when`(mockCallLogCursor.getInt(1)).thenAnswer { types[currentRow] }
        Mockito.`when`(mockCallLogCursor.getLong(2)).thenAnswer { durations[currentRow] }
        Mockito.`when`(mockCallLogCursor.getLong(3)).thenAnswer { dates[currentRow] }
        Mockito.`when`(mockCallLogCursor.count).thenReturn(100)

        // Mock ContactHelper lookups using a projection-based answer
        var phoneLookupCount = 0
        var nameLookupCount = 0

        Mockito.`when`(
            mockContentResolver.query(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenAnswer { invocation ->
            val projection = invocation.arguments[1] as? Array<*>
            val projectionList = projection?.toList() ?: emptyList<Any?>()
            if (projectionList.contains("date")) {
                mockCallLogCursor
            } else if (projectionList.contains("_id")) {
                phoneLookupCount++
                val contactId = phoneLookupCount.toString()
                val mockLookupCursor = mock<Cursor>()
                Mockito.`when`(mockLookupCursor.moveToFirst()).thenReturn(true)
                Mockito.`when`(mockLookupCursor.getColumnIndex(Mockito.anyString())).thenReturn(0)
                Mockito.`when`(mockLookupCursor.getString(0)).thenReturn(contactId)
                mockLookupCursor
            } else {
                nameLookupCount++
                val contactId = nameLookupCount.toString()
                val name = "Contact $contactId"
                val mockNameCursor = mock<Cursor>()
                Mockito.`when`(mockNameCursor.moveToFirst()).thenReturn(true)
                Mockito.`when`(mockNameCursor.getColumnIndex(Mockito.anyString())).thenReturn(0)
                Mockito.`when`(mockNameCursor.getString(0)).thenReturn(name)
                mockNameCursor
            }
        }

        Mockito.`when`(mockPrefs.getInt(Mockito.anyString(), Mockito.anyInt())).thenReturn(0)
        Mockito.`when`(mockPrefs.all).thenReturn(emptyMap<String, Any>())

        // Calculate expected scores using chronological forward reference loop
        val contactScoresRef = mutableMapOf<String, Int>()
        val numberToId =
            mapOf(
                "+15550001" to "1",
                "+15550002" to "2",
                "+15550003" to "3",
            )
        for (i in 99 downTo 0) {
            val number = numbers[i]
            val type = types[i]
            val duration = durations[i]
            val contactId = numberToId[number]!!
            val currentScore = contactScoresRef[contactId] ?: 0
            var newScore = currentScore
            when (type) {
                CallLog.Calls.MISSED_TYPE -> newScore += 1
                CallLog.Calls.REJECTED_TYPE -> newScore += 2
                CallLog.Calls.INCOMING_TYPE -> {
                    if (duration > 0) newScore = 0 else newScore += 1
                }
                CallLog.Calls.OUTGOING_TYPE -> {
                    if (duration > 0) newScore = 0
                }
            }
            contactScoresRef[contactId] = newScore
        }

        // Run sync
        CallSyncHelper.syncCallLogs(mockContext)

        // Verify final scores match the reference calculations
        for ((contactId, expectedScore) in contactScoresRef) {
            if (expectedScore > 0) {
                Mockito.verify(mockEditor).putInt(eq("score_$contactId"), eq(expectedScore))
            }
        }
        Mockito.verify(mockEditor, Mockito.atLeastOnce()).apply()
    }
}
