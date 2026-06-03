package com.nihaltp.smartringtone.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihaltp.smartringtone.R
import com.nihaltp.smartringtone.data.CallLogEntry
import com.nihaltp.smartringtone.data.Contact
import com.nihaltp.smartringtone.data.Ringtone
import java.text.SimpleDateFormat
import java.util.*

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

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

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
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = stringResource(R.string.tab_ringtones)) },
                    label = { Text(stringResource(R.string.tab_ringtones)) },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentColor,
                            selectedTextColor = AccentColor,
                            indicatorColor = AccentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = stringResource(R.string.tab_contacts)) },
                    label = { Text(stringResource(R.string.tab_contacts)) },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentColor,
                            selectedTextColor = AccentColor,
                            indicatorColor = AccentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.tab_log)) },
                    label = { Text(stringResource(R.string.tab_log)) },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentColor,
                            selectedTextColor = AccentColor,
                            indicatorColor = AccentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
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
                        0 ->
                            RingtonesTab(
                                ringtones = ringtones,
                                playingUri = playingUri,
                                onAdd = { audioPickerLauncher.launch("audio/*") },
                                onDelete = { id -> viewModel.deleteRingtone(id) },
                                onMove = { index, up -> viewModel.moveRingtone(index, up) },
                                onTogglePlay = { uri -> viewModel.togglePlayPreview(uri) },
                            )
                        1 ->
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
                        2 ->
                            CallLogsTab(
                                callLogs = callLogs,
                                onClear = { viewModel.clearHistory() },
                            )
                    }
                }
            }
        }
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

@Composable
fun RingtonesTab(
    ringtones: List<Ringtone>,
    playingUri: String?,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onMove: (Int, Boolean) -> Unit,
    onTogglePlay: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ringtone_sequence),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (ringtones.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_ringtones),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(ringtones) { index, ringtone ->
                    RingtoneCard(
                        ringtone = ringtone,
                        index = index,
                        isPlaying = playingUri == ringtone.uri,
                        isFirst = index == 0,
                        isLast = index == ringtones.size - 1,
                        onDelete = { onDelete(ringtone.id) },
                        onMove = { up -> onMove(index, up) },
                        onTogglePlay = { onTogglePlay(ringtone.uri) },
                    )
                }
            }
        }
    }
}

@Composable
fun RingtoneCard(
    ringtone: Ringtone,
    index: Int,
    isPlaying: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onDelete: () -> Unit,
    onMove: (Boolean) -> Unit,
    onTogglePlay: () -> Unit,
) {
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
                    .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Index Badge (Utility look: square, thin border)
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .background(
                            color = if (isPlaying) AccentColor.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = if (isPlaying) AccentColor else BorderColor,
                            shape = RoundedCornerShape(4.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = String.format("%02d", index + 1),
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) AccentColor else TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ringtone.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.plays_on_score, index + 1),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Play/Pause
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription =
                        if (isPlaying) {
                            stringResource(
                                R.string.content_desc_pause,
                            )
                        } else {
                            stringResource(R.string.content_desc_play)
                        },
                    tint = if (isPlaying) AccentColor else TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Reorder Buttons
            IconButton(onClick = { onMove(true) }, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.content_desc_move_up),
                    tint = if (isFirst) TextSecondary.copy(alpha = 0.2f) else TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = { onMove(false) }, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.content_desc_move_down),
                    tint = if (isLast) TextSecondary.copy(alpha = 0.2f) else TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.content_desc_delete),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(
    contacts: List<Contact>,
    ringtones: List<Ringtone>,
    searchQuery: String,
    playingUri: String?,
    onSearchChange: (String) -> Unit,
    onResetScore: (String) -> Unit,
    onResetAll: () -> Unit,
    onTogglePlay: (String) -> Unit,
) {
    val filteredContacts =
        remember(contacts, searchQuery) {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
            }
        }

    var showResetAllDialog by remember { mutableStateOf(false) }
    var contactToReset by remember { mutableStateOf<Contact?>(null) }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.reset_all_confirm_title),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_all_confirm_message),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetAll()
                        showResetAllDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.reset),
                        color = AccentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = TextSecondary,
                    )
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }

    if (contactToReset != null) {
        AlertDialog(
            onDismissRequest = { contactToReset = null },
            title = {
                Text(
                    text = stringResource(R.string.reset_contact_confirm_title),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_contact_confirm_message, contactToReset?.name ?: ""),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        contactToReset?.let { onResetScore(it.id) }
                        contactToReset = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.reset),
                        color = AccentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToReset = null }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = TextSecondary,
                    )
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Search & Global Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search_contacts), color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors =
                    TextFieldDefaults.textFieldColors(
                        containerColor = CardBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                shape = RoundedCornerShape(6.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = { showResetAllDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Icon(Icons.Default.RotateLeft, contentDescription = null, tint = AccentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.reset_all), color = AccentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredContacts.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_contacts),
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredContacts) { contact ->
                    ContactCard(
                        contact = contact,
                        ringtones = ringtones,
                        isPlaying = contact.currentRingtone != null && playingUri == contact.currentRingtone,
                        onResetScore = { contactToReset = contact },
                        onTogglePlay = {
                            contact.currentRingtone?.let { onTogglePlay(it) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ContactCard(
    contact: Contact,
    ringtones: List<Ringtone>,
    isPlaying: Boolean,
    onResetScore: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (contact.score > 0) {
                        Modifier.clickable { onResetScore() }
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = contact.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (contact.phone.isNotEmpty()) {
                            Text(
                                text = contact.phone,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .border(1.dp, if (contact.score > 0) AccentColor.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "SCORE: ${contact.score}",
                        color = if (contact.score > 0) AccentColor else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (contact.score > 0) AccentColor else TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val displayName =
                        when (contact.mappedRingtoneName) {
                            "Original (Custom)" -> stringResource(R.string.original_custom)
                            "System Default" -> stringResource(R.string.system_default)
                            else -> contact.mappedRingtoneName ?: stringResource(R.string.system_default)
                        }
                    Text(
                        text = stringResource(R.string.active_ringtone, displayName),
                        color = if (contact.score > 0) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (contact.currentRingtone != null) {
                        IconButton(onClick = onTogglePlay, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.content_desc_play_custom_ringtone),
                                tint = if (isPlaying) AccentColor else TextPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (contact.score > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onResetScore,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentColor),
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.reset), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogsTab(
    callLogs: List<CallLogEntry>,
    onClear: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tracked_call_history),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            if (callLogs.isNotEmpty()) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.clear_logs), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (callLogs.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_logs),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(callLogs) { entry ->
                    CallLogCard(entry = entry)
                }
            }
        }
    }
}

@Composable
fun CallLogCard(entry: CallLogEntry) {
    val callIcon =
        when (entry.direction) {
            "INCOMING" -> Icons.Default.CallReceived
            else -> Icons.Default.CallMade
        }

    val typeColor =
        when (entry.type) {
            "ANSWERED" -> AccentColor
            else -> TextSecondary
        }

    val timeString =
        remember(entry.timestamp) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(entry.timestamp))
        }

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
                    .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Icon(
                imageVector = callIcon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(16.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.name.isNotEmpty()) entry.name else entry.number,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val typeText =
                    when (entry.type) {
                        "ANSWERED" -> stringResource(R.string.answered)
                        "MISSED" -> stringResource(R.string.missed)
                        "REJECTED" -> stringResource(R.string.rejected)
                        else -> stringResource(R.string.unanswered)
                    }
                Text(
                    text = "$timeString • $typeText",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Score Change Badge
            Box(
                modifier =
                    Modifier
                        .border(
                            width = 1.dp,
                            color = if (entry.scoreChange.startsWith("+")) AccentColor.copy(alpha = 0.5f) else BorderColor,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                val changeText = if (entry.scoreChange == "Reset to 0") stringResource(R.string.reset).uppercase() else entry.scoreChange
                Text(
                    text = changeText,
                    color = if (entry.scoreChange.startsWith("+")) AccentColor else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
