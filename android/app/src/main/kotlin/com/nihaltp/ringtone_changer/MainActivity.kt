package com.nihaltp.ringtone_changer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val CHANNEL = "call_state_channel"
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private lateinit var telephonyManager: TelephonyManager
    private var methodChannel: MethodChannel? = null
    private var phoneStateListener: PhoneStateListener? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        )

        telephonyManager =
            getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        methodChannel?.setMethodCallHandler { call, result ->
            if (call.method == "startListening") {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startListening()
                    result.success(null)
                } else {
                    result.error("PERMISSION_DENIED", "Phone state permission not granted", null)
                }
            } else if (call.method == "canWriteSettings") {
                result.success(Settings.System.canWrite(this))
            } else if (call.method == "openWriteSettings") {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                result.success(null)
            } else {
                result.notImplemented()
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        }
    }

    private fun startListening() {
        if (phoneStateListener != null) return

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (lastState == TelephonyManager.CALL_STATE_RINGING &&
                    state == TelephonyManager.CALL_STATE_OFFHOOK
                ) {
                    methodChannel?.invokeMethod("callAccepted", null)
                }

                if (lastState == TelephonyManager.CALL_STATE_RINGING &&
                    state == TelephonyManager.CALL_STATE_IDLE
                ) {
                    methodChannel?.invokeMethod("callRejected", null)
                }

                lastState = state
            }
        }
        
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }
}
