package com.nihaltp.smartringtone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nihaltp.smartringtone.data.CallSyncHelper
import com.nihaltp.smartringtone.ui.MainScreen
import com.nihaltp.smartringtone.ui.RingtoneChangerViewModel
import com.nihaltp.smartringtone.ui.SmartRingtoneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel: RingtoneChangerViewModel by viewModels()
    private val hasPermissionsState = mutableStateOf(false)

    private val requiredPermissions by lazy {
        val list =
            mutableListOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
            )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            hasPermissionsState.value = allGranted
            if (allGranted) {
                triggerSyncAndLoad()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (com.nihaltp.smartringtone.data.PreferenceHelper.isScreenshotMode(this)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        checkPermissions()

        setContent {
            val themeMode by viewModel.theme.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            SmartRingtoneTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
                MainScreen(
                    viewModel = viewModel,
                    hasPermissions = hasPermissionsState.value,
                    onRequestPermissions = { requestPermissions() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        if (hasPermissionsState.value) {
            triggerSyncAndLoad()
        }
    }

    private fun checkPermissions() {
        val allGranted =
            com.nihaltp.smartringtone.data.PreferenceHelper.isScreenshotMode(this) ||
                requiredPermissions.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }
        hasPermissionsState.value = allGranted
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(requiredPermissions)
    }

    private fun triggerSyncAndLoad() {
        if (com.nihaltp.smartringtone.data.PreferenceHelper.isScreenshotMode(this)) {
            viewModel.loadData()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Sync any call log entries that occurred in the background
                CallSyncHelper.syncCallLogs(this@MainActivity)
            }
            // Reload local data in the viewmodel
            viewModel.loadData()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPreview()
    }
}
