package com.nihaltp.smartringtone.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class CallSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("CallSyncWorker", "Starting background call log sync Work...")
        AppLogger.log(applicationContext, "CallSyncWorker", "Starting background call log sync Work...")
        return try {
            // Wait 1.5 seconds to let the dialer finish writing the call log
            delay(1500)
            CallSyncHelper.syncCallLogs(applicationContext)
            AppLogger.log(applicationContext, "CallSyncWorker", "Background call log sync Work completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("CallSyncWorker", "Error syncing call logs in Worker", e)
            AppLogger.log(applicationContext, "CallSyncWorker", "Error syncing call logs in Worker: ${e.localizedMessage ?: e.message}")
            Result.failure()
        }
    }
}
