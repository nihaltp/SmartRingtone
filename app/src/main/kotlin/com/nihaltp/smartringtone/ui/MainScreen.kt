package com.nihaltp.smartringtone.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihaltp.smartringtone.R
import com.nihaltp.smartringtone.data.AppLogger

// Technical dark-mode color scheme with clean blue utility accent
val BackgroundColor = Color(0xFF121214)
val CardBackground = Color(0xFF1C1C1E)
val BorderColor = Color(0xFF2C2C30)
val AccentColor = Color(0xFF007ACC) // VS Code/Technical Blue
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: RingtoneChangerViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
) {
    val ringtones by viewModel.ringtones.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playingUri by viewModel.playingUri.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoggingEnabled by viewModel.isLoggingEnabled.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val versionName = remember { getAppVersionName(context) }
    val versionCode = remember { getAppVersionCode(context) }

    var selectedTab by remember { mutableStateOf(AppTab.RINGTONES) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(isLoggingEnabled) {
        if (!isLoggingEnabled && selectedTab == AppTab.LOG) {
            selectedTab = AppTab.RINGTONES
        }
    }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.addRingtone(uri)
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                tonalElevation = 8.dp,
            ) {
                val tabColors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentColor,
                        selectedTextColor = AccentColor,
                        indicatorColor = AccentColor.copy(alpha = 0.05f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                    )

                NavigationBarItem(
                    selected = selectedTab == AppTab.RINGTONES,
                    onClick = { selectedTab = AppTab.RINGTONES },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = stringResource(R.string.tab_ringtones)) },
                    label = { Text(stringResource(R.string.tab_ringtones)) },
                    colors = tabColors,
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.CONTACTS,
                    onClick = { selectedTab = AppTab.CONTACTS },
                    icon = { Icon(Icons.Default.People, contentDescription = stringResource(R.string.tab_contacts)) },
                    label = { Text(stringResource(R.string.tab_contacts)) },
                    colors = tabColors,
                )
                if (isLoggingEnabled) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.LOG,
                        onClick = { selectedTab = AppTab.LOG },
                        icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.tab_log)) },
                        label = { Text(stringResource(R.string.tab_log)) },
                        colors = tabColors,
                    )
                }
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings)) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    colors = tabColors,
                )
            }
        },
    ) { innerPadding ->
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BackgroundColor)
                    .padding(innerPadding),
            color = BackgroundColor,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Panel
                HeaderPanel(
                    isLoading = isLoading,
                    onSync = { viewModel.syncCallLogs() },
                    hasPermissions = hasPermissions,
                    onRequestPermissions = onRequestPermissions,
                )

                if (!hasPermissions) {
                    // Educational Permission Card
                    PermissionPlaceholderCard(onRequestPermissions)
                } else {
                    // Tab Contents
                    when (selectedTab) {
                        AppTab.RINGTONES ->
                            RingtonesTab(
                                ringtones = ringtones,
                                playingUri = playingUri,
                                onAdd = { audioPickerLauncher.launch("audio/*") },
                                onDelete = { id -> viewModel.deleteRingtone(id) },
                                onMove = { index, up -> viewModel.moveRingtone(index, up) },
                                onTogglePlay = { uri -> viewModel.togglePlayPreview(uri) },
                            )
                        AppTab.CONTACTS ->
                            ContactsTab(
                                contacts = contacts,
                                ringtones = ringtones,
                                searchQuery = searchQuery,
                                playingUri = playingUri,
                                onSearchChange = { searchQuery = it },
                                onResetScore = { id -> viewModel.resetContactScore(id) },
                                onResetAll = { viewModel.resetAllScores() },
                                onTogglePlay = { uri -> viewModel.togglePlayPreview(uri) },
                            )
                        AppTab.LOG ->
                            CallLogsTab(
                                callLogs = callLogs,
                                onClear = { viewModel.clearHistory() },
                            )
                        AppTab.SETTINGS ->
                            SettingsTab(
                                viewModel = viewModel,
                            )
                    }
                }
            }
        }
    }

    if (error != null) {
        var showStackTrace by remember { mutableStateOf(false) }
        val err = error!!
        val stackTrace = remember(err) { android.util.Log.getStackTraceString(err) }

        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.error_occurred),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                    )
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = err.localizedMessage ?: err.message ?: "An unexpected error occurred.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showStackTrace = !showStackTrace },
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentColor),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = if (showStackTrace) "Hide Stack Trace" else "Show Stack Trace",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (showStackTrace) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = stackTrace,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier =
                                    Modifier
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = "Crash/Error: ${err.message?.take(50) ?: "Unknown Error"}"
                        val body =
                            """
                            **Error Description:**
                            ${err.localizedMessage ?: err.message ?: "No error message provided."}

                            **Stack Trace:**
                            ```
                            $stackTrace
                            ```

                            **Device & App Details:**
                            - Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                            - Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
                            - App Version: $versionName ($versionCode)
                            """.trimIndent()

                        val reportUrl =
                            "https://github.com/nihaltp/SmartRingtone/issues/new" +
                                "?title=${Uri.encode(title)}" +
                                "&body=${Uri.encode(body)}"

                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            AppLogger.log(context, "MainScreen", "Failed to open GitHub issues link", e)
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.error_report_github),
                        color = AccentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val details = "Error: ${err.localizedMessage ?: err.message}\n\nStack Trace:\n$stackTrace"
                            clipboardManager.setText(AnnotatedString(details))
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.error_copy),
                            color = TextSecondary,
                        )
                    }
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(
                            text = stringResource(R.string.error_dismiss),
                            color = TextSecondary,
                        )
                    }
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }
}

@Composable
fun HeaderPanel(
    isLoading: Boolean,
    onSync: () -> Unit,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(vertical = 14.dp, horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasPermissions) {
                    IconButton(onClick = onSync, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentColor,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.content_desc_sync_logs),
                                tint = TextPrimary,
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentColor),
                    ) {
                        Text(stringResource(R.string.setup_permissions), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionPlaceholderCard(onRequestPermissions: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = stringResource(R.string.permissions_required),
            modifier = Modifier.size(48.dp),
            tint = TextSecondary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permissions_required),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permissions_desc),
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(stringResource(R.string.grant_permissions), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
