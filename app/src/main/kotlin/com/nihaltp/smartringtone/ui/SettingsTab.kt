package com.nihaltp.smartringtone.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihaltp.smartringtone.BuildConfig
import com.nihaltp.smartringtone.R
import com.nihaltp.smartringtone.data.GitHubIssueHelper

@Composable
fun SettingsTab(viewModel: RingtoneChangerViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isLoggingEnabled by viewModel.isLoggingEnabled.collectAsState()
    val logsText by viewModel.logsText.collectAsState()
    val isAppPaused by viewModel.isAppPaused.collectAsState()

    val backupFileUri by viewModel.backupFileUri.collectAsState()
    val backupFileName by viewModel.backupFileName.collectAsState()

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.exportData(uri) { success ->
                    val msg =
                        if (success) {
                            context.getString(R.string.export_success)
                        } else {
                            context.getString(R.string.export_failed)
                        }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.importData(uri) { success ->
                    val msg =
                        if (success) {
                            context.getString(R.string.import_success)
                        } else {
                            context.getString(R.string.import_failed)
                        }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

    var showLicensesDialog by remember { mutableStateOf(false) }
    var showLogsViewer by remember { mutableStateOf(false) }

    val versionName = remember { GitHubIssueHelper.getAppVersionName(context) }
    val versionCode = remember { GitHubIssueHelper.getAppVersionCode(context) }
    val installSource = remember { GitHubIssueHelper.getInstallSource(context) }
    val downloadSource = remember { GitHubIssueHelper.getDownloadSource(context) }

    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }

    if (showLogsViewer) {
        LogViewerDialog(
            logsText = logsText,
            onRefresh = { viewModel.loadLogs() },
            onClear = { viewModel.clearLogs() },
            onCopy = {
                clipboardManager.setText(AnnotatedString(logsText))
            },
            onDismiss = { showLogsViewer = false },
        )
    }

    val currentTheme by viewModel.theme.collectAsState()
    val scoreAdditionMissed by viewModel.scoreAdditionMissed.collectAsState()
    val scoreAdditionRejected by viewModel.scoreAdditionRejected.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Pause App Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_pause),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAppPaused(!isAppPaused) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.enable_pause),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.enable_pause_desc),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isAppPaused,
                        onCheckedChange = { viewModel.setAppPaused(it) },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentColor,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BorderColor,
                            ),
                    )
                }
            }
        }

        // Theme Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(12.dp))

                ThemeOptionRow(
                    text = stringResource(R.string.theme_light),
                    selected = currentTheme == "light",
                    onClick = { viewModel.setTheme("light") },
                )
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                ThemeOptionRow(
                    text = stringResource(R.string.theme_dark),
                    selected = currentTheme == "dark",
                    onClick = { viewModel.setTheme("dark") },
                )
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                ThemeOptionRow(
                    text = stringResource(R.string.theme_system),
                    selected = currentTheme == "system",
                    onClick = { viewModel.setTheme("system") },
                )
            }
        }

        // Call Score Additions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_score_additions).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_score_additions_desc),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                ScoreAdditionRow(
                    title = stringResource(R.string.score_addition_missed_calls),
                    value = scoreAdditionMissed,
                    onValueChange = { viewModel.setScoreAdditionMissed(it) },
                )
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                ScoreAdditionRow(
                    title = stringResource(R.string.score_addition_rejected_calls),
                    value = scoreAdditionRejected,
                    onValueChange = { viewModel.setScoreAdditionRejected(it) },
                )
            }
        }

        // Fallback Default Ringtone Card
        val fallbackName by viewModel.fallbackRingtoneName.collectAsState()
        val fallbackPickerLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
            ) { uri: Uri? ->
                if (uri != null) {
                    viewModel.setFallbackRingtone(uri)
                }
            }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_fallback_ringtone),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_fallback_ringtone_desc),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = fallbackName ?: stringResource(R.string.fallback_ringtone_not_set),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (fallbackName != null) TextPrimary else TextSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { fallbackPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.select_fallback_ringtone_btn), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (fallbackName != null) {
                        OutlinedButton(
                            onClick = { viewModel.clearFallbackRingtone() },
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.clear_fallback_ringtone_btn), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_backup_restore),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_backup_restore_desc),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(BackgroundColor, shape = RoundedCornerShape(4.dp))
                            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f).clickable {
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                        ) {
                            Text(
                                text = stringResource(R.string.backup_file_label).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = backupFileName ?: stringResource(R.string.backup_file_not_set),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (backupFileName != null) TextPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (backupFileUri != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.clearBackupFileUri() },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear backup file path",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            if (backupFileUri != null) {
                                viewModel.exportDataDirect { success ->
                                    val msg =
                                        if (success) {
                                            context.getString(R.string.export_success)
                                        } else {
                                            context.getString(R.string.export_failed)
                                        }
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val backupFileName =
                                    if (BuildConfig.DEBUG) {
                                        "smartringtone_backup.debug.json"
                                    } else {
                                        "smartringtone_backup.json"
                                    }
                                exportLauncher.launch(backupFileName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export_data_btn), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (backupFileUri != null) {
                                viewModel.importDataDirect { success ->
                                    val msg =
                                        if (success) {
                                            context.getString(R.string.import_success)
                                        } else {
                                            context.getString(R.string.import_failed)
                                        }
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.import_data_btn), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Logging Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_logging),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLoggingEnabled(!isLoggingEnabled) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.enable_logging),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.enable_logging_desc),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = { viewModel.setLoggingEnabled(it) },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentColor,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BorderColor,
                            ),
                    )
                }

                if (isLoggingEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.loadLogs()
                                showLogsViewer = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.view_logs), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearLogs() },
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.clear_logs_btn), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Open Source Licenses Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showLicensesDialog = true }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.licenses_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // GitHub Links Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_links),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // GitHub Repo
                SettingsLinkRow(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.github_repo_title),
                    subtitle = "https://github.com/nihaltp/SmartRingtone",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nihaltp/SmartRingtone"))
                        context.startActivity(intent)
                    },
                )

                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

                // GitHub Issues
                SettingsLinkRow(
                    icon = Icons.Default.BugReport,
                    title = stringResource(R.string.github_issues_title),
                    subtitle = stringResource(R.string.github_issues_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nihaltp/SmartRingtone/issues"))
                        context.startActivity(intent)
                    },
                )
            }
        }

        // App Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_app_info),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Version Name box
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(BackgroundColor, shape = RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.version_title).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = versionName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }

                    // Version Code box
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(BackgroundColor, shape = RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.version_code).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = versionCode.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Install Source box
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(BackgroundColor, shape = RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.installer).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = installSource,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Download Source box
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(BackgroundColor, shape = RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.download_source).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = downloadSource,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.licenses_title),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.third_party_licenses),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor,
                    fontFamily = FontFamily.Monospace,
                )

                LicenseItem(
                    name = stringResource(R.string.lic_jetpack),
                    license = stringResource(R.string.lic_apache_desc),
                )

                Divider(color = BorderColor)

                LicenseItem(
                    name = stringResource(R.string.lic_kotlin),
                    license = stringResource(R.string.lic_apache_desc),
                )

                Divider(color = BorderColor)

                LicenseItem(
                    name = stringResource(R.string.lic_gson),
                    license = stringResource(R.string.lic_apache_desc),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    color = AccentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
    )
}

@Composable
fun LicenseItem(
    name: String,
    license: String,
) {
    Column {
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = license,
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 16.sp,
        )
    }
}

@Composable
fun LogViewerDialog(
    logsText: String,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.logs_viewer_title),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Logs",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (logsText.trim().isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                                .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.logs_empty),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                                .padding(8.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            SelectionContainer {
                                Text(
                                    text = logsText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        onClear()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.clear_logs_btn),
                        color = Color.Red,
                        fontSize = 13.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (logsText.trim().isNotEmpty()) {
                        TextButton(
                            onClick = {
                                onCopy()
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.copied_to_clipboard),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.copy_to_clipboard),
                                color = AccentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.close),
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        },
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
    )
}

@Composable
fun ThemeOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = AccentColor,
                    unselectedColor = TextSecondary,
                ),
        )
    }
}

@Composable
fun ScoreAdditionRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )

        if (!isExpanded) {
            Text(
                text = value.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AccentColor,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(enabled = false) { },
            ) {
                IconButton(
                    onClick = { if (value > 0) onValueChange(value - 1) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = AccentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = value.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                IconButton(
                    onClick = { onValueChange(value + 1) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = AccentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
