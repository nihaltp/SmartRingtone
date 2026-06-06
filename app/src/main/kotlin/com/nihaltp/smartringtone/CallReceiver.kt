package com.nihaltp.smartringtone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nihaltp.smartringtone.data.AppLogger
import com.nihaltp.smartringtone.data.CallSyncWorker

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("CallReceiver", "Phone state changed: $state")
            AppLogger.log(context, "CallReceiver", "Phone state changed: $state")

            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended, missed, or rejected. Schedule sync job via WorkManager.
                Log.d("CallReceiver", "Scheduling CallSyncWorker unique work...")
                AppLogger.log(context, "CallReceiver", "Scheduling CallSyncWorker unique work...")
                try {
                    val workRequest = OneTimeWorkRequestBuilder<CallSyncWorker>().build()
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "CallSyncWork",
                        ExistingWorkPolicy.REPLACE,
                        workRequest,
                    )
                } catch (e: Exception) {
                    Log.e("CallReceiver", "Failed to enqueue WorkManager job", e)
                    AppLogger.log(context, "CallReceiver", "Failed to enqueue WorkManager job: ${e.message}", e)
                }
            }
        }
    }
}
