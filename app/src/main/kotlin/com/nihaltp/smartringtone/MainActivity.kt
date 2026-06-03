package com.nihaltp.smartringtone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nihaltp.smartringtone.data.CallSyncHelper
import com.nihaltp.smartringtone.ui.MainScreen
import com.nihaltp.smartringtone.ui.RingtoneChangerViewModel
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

        checkPermissions()

        setContent {
            MainScreen(
                viewModel = viewModel,
                hasPermissions = hasPermissionsState.value,
                onRequestPermissions = { requestPermissions() },
            )
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
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        hasPermissionsState.value = allGranted
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(requiredPermissions)
    }

    private fun triggerSyncAndLoad() {
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
