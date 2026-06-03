package com.nihaltp.smartringtone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.nihaltp.smartringtone.data.CallSyncHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("CallReceiver", "Phone state changed: $state")

            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended, missed, or rejected.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Wait a brief moment to allow the dialer app to write the call log entry
                        delay(1500)
                        Log.d("CallReceiver", "Syncing call logs after call completion...")
                        CallSyncHelper.syncCallLogs(context)
                    } catch (e: Exception) {
                        Log.e("CallReceiver", "Error in CallReceiver sync", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
