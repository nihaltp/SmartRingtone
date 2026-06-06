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
        Mockito.`when`(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
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

    @Test
    fun testExportPreferencesExcludesBackupFileUri() {
        val testPrefs =
            mapOf(
                "app_theme" to "dark",
                "backup_file_uri" to "content://some/backup/file.json",
                "score_contact_1" to 5,
            )
        Mockito.`when`(mockPrefs.all).thenReturn(testPrefs)

        val exportedJson = PreferenceHelper.exportPreferences(mockContext)
        org.junit.Assert.assertTrue(exportedJson.contains("dark"))
        org.junit.Assert.assertTrue(exportedJson.contains("score_contact_1"))
        org.junit.Assert.assertFalse(exportedJson.contains("backup_file_uri"))
    }

    @Test
    fun testImportPreferencesRestoresValuesAndPreservesBackupFileUri() {
        val importedJson = """{"app_theme":"light","score_contact_1":3,"app_paused":true}"""

        Mockito.`when`(mockPrefs.getString("backup_file_uri", null)).thenReturn("content://saved/uri")
        Mockito.`when`(mockPrefs.all).thenReturn(emptyMap())

        val success = PreferenceHelper.importPreferences(mockContext, importedJson)

        org.junit.Assert.assertTrue(success)
        Mockito.verify(mockEditor).clear()
        Mockito.verify(mockEditor).putString("backup_file_uri", "content://saved/uri")
        Mockito.verify(mockEditor).putString("app_theme", "light")
        Mockito.verify(mockEditor).putInt("score_contact_1", 3)
        Mockito.verify(mockEditor).putBoolean("app_paused", true)
        Mockito.verify(mockEditor, Mockito.atLeastOnce()).apply()
    }

    @Test
    fun testResolvedRingtoneForScore() {
        val ringtoneA = Ringtone(1, "Ringtone A", "content://rt_a")
        val ringtoneBlank = Ringtone(2, "Blank Ringtone", "blank")
        val ringtoneB = Ringtone(3, "Ringtone B", "content://rt_b")
        val ringtoneBlank2 = Ringtone(4, "Blank Ringtone", "blank")

        val ringtones = listOf(ringtoneA, ringtoneBlank, ringtoneB, ringtoneBlank2)

        // 1. Score 0 or less should return null
        assertEquals(null, ContactHelper.getResolvedRingtoneForScore(0, ringtones))
        assertEquals(null, ContactHelper.getResolvedRingtoneForScore(-1, ringtones))

        // 2. Score 1 (index 0: Ringtone A) -> Ringtone A
        assertEquals(ringtoneA, ContactHelper.getResolvedRingtoneForScore(1, ringtones))

        // 3. Score 2 (index 1: blank) -> should fall back to Ringtone A
        assertEquals(ringtoneA, ContactHelper.getResolvedRingtoneForScore(2, ringtones))

        // 4. Score 3 (index 2: Ringtone B) -> Ringtone B
        assertEquals(ringtoneB, ContactHelper.getResolvedRingtoneForScore(3, ringtones))

        // 5. Score 4 (index 3: blank) -> should fall back to Ringtone B
        assertEquals(ringtoneB, ContactHelper.getResolvedRingtoneForScore(4, ringtones))

        // 6. Score 5 (coerced to index 3: blank) -> should fall back to Ringtone B
        assertEquals(ringtoneB, ContactHelper.getResolvedRingtoneForScore(5, ringtones))

        // 7. If all preceding are blank
        val allBlankRingtones = listOf(ringtoneBlank, ringtoneBlank2)
        assertEquals(null, ContactHelper.getResolvedRingtoneForScore(1, allBlankRingtones))
        assertEquals(null, ContactHelper.getResolvedRingtoneForScore(2, allBlankRingtones))
    }

    @Test
    fun testLogTabEnabledPreferences() {
        Mockito.`when`(mockPrefs.getBoolean(org.mockito.kotlin.eq("log_tab_enabled"), org.mockito.kotlin.eq(true))).thenReturn(true)
        val defaultVal = PreferenceHelper.isLogTabEnabled(mockContext)
        assertEquals(true, defaultVal)

        PreferenceHelper.setLogTabEnabled(mockContext, false)
        Mockito.verify(mockEditor).putBoolean(org.mockito.kotlin.eq("log_tab_enabled"), org.mockito.kotlin.eq(false))
        Mockito.verify(mockEditor).apply()
    }
}
