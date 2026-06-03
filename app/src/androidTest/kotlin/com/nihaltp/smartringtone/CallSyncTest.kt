package com.nihaltp.smartringtone

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.nihaltp.smartringtone.data.CallSyncHelper
import com.nihaltp.smartringtone.data.PreferenceHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallSyncTest {
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG,
        )

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testProcessCallRejectedIncrementsScoreByTwo() {
        val testName = "Test CallSync Contact"
        val testNumber = "+15550199"
        val contactId = insertTestContact(context, testName, testNumber)

        try {
            // Set initial score to 0
            PreferenceHelper.setContactScore(context, contactId, 0)

            // Process a rejected call
            CallSyncHelper.processCall(
                context = context,
                number = testNumber,
                type = CallLog.Calls.REJECTED_TYPE,
                duration = 0L,
                date = System.currentTimeMillis(),
            )

            // Assert score is incremented by 2
            val updatedScore = PreferenceHelper.getContactScore(context, contactId)
            assertEquals(2, updatedScore)
        } finally {
            // Clean up
            deleteContact(context, contactId)
            PreferenceHelper.clearContactData(context, contactId)
        }
    }

    @Test
    fun testSyncCallLogsWatermarkDoesNotAdvanceIfNoNewLogs() {
        val maxDate = getNewestCallLogDate()
        if (maxDate != null) {
            // Set sync time to the exact date of the newest call log
            PreferenceHelper.setLastSyncTime(context, maxDate)

            // Run sync
            CallSyncHelper.syncCallLogs(context)

            // The sync time should NOT have advanced because there are no logs newer than maxDate
            assertEquals(maxDate, PreferenceHelper.getLastSyncTime(context))
        }
    }

    @Test
    fun testSyncCallLogsWatermarkAdvancesToNewestLog() {
        val maxDate = getNewestCallLogDate()
        if (maxDate != null) {
            // Set sync time to 1 second before the newest call log
            val initialSyncTime = maxDate - 1000
            PreferenceHelper.setLastSyncTime(context, initialSyncTime)

            // Run sync
            CallSyncHelper.syncCallLogs(context)

            // The sync time should have advanced exactly to maxDate (since that's the newest processed log)
            assertEquals(maxDate, PreferenceHelper.getLastSyncTime(context))
        }
    }

    private fun getNewestCallLogDate(): Long? {
        val projection = arrayOf(CallLog.Calls.DATE)
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        return try {
            val cursor =
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder,
                )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(0)
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun insertTestContact(
        context: Context,
        name: String,
        number: String,
    ): String {
        val contentResolver = context.contentResolver
        val ops = arrayListOf<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build(),
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build(),
        )

        val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        val rawContactUri = results[0].uri ?: throw Exception("Failed to insert raw contact")
        val rawContactId = ContentUris.parseId(rawContactUri)

        var contactId: String? = null
        val cursor =
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.CONTACT_ID),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ?",
                arrayOf(rawContactId.toString()),
                null,
            )
        cursor?.use {
            if (it.moveToFirst()) {
                contactId = it.getString(0)
            }
        }
        return contactId ?: throw Exception("Failed to get contact ID for raw contact ID $rawContactId")
    }

    private fun deleteContact(
        context: Context,
        contactId: String,
    ) {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
        context.contentResolver.delete(contactUri, null, null)
    }
}
