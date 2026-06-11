package com.nihaltp.smartringtone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihaltp.smartringtone.R
import com.nihaltp.smartringtone.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(
    contacts: List<Contact>,
    blockedContacts: Set<String>,
    ringtones: List<Ringtone>,
    searchQuery: String,
    playingUri: String?,
    onSearchChange: (String) -> Unit,
    onResetScore: (String) -> Unit,
    onResetAll: () -> Unit,
    onTogglePlay: (String) -> Unit,
    onRescanContact: (String) -> Unit,
) {
    var sortBy by rememberSaveable { mutableStateOf(ContactSortOrder.SCORE) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }

    val sortedAndFilteredContacts =
        remember(contacts, blockedContacts, searchQuery, sortBy, sortAscending) {
            val filtered =
                ContactSearchHelper.filterContacts(contacts, searchQuery)
                    .filter { !blockedContacts.contains(it.id) }
            when (sortBy) {
                ContactSortOrder.NAME -> {
                    if (sortAscending) {
                        filtered.sortedWith(
                            compareBy<Contact> { it.name.lowercase() }
                                .thenByDescending { it.score },
                        )
                    } else {
                        filtered.sortedWith(
                            compareByDescending<Contact> { it.name.lowercase() }
                                .thenByDescending { it.score },
                        )
                    }
                }
                ContactSortOrder.SCORE -> {
                    if (sortAscending) {
                        filtered.sortedWith(
                            compareBy<Contact> { it.score }
                                .thenBy { it.name.lowercase() },
                        )
                    } else {
                        filtered.sortedWith(
                            compareByDescending<Contact> { it.score }
                                .thenBy { it.name.lowercase() },
                        )
                    }
                }
            }
        }

    var showResetAllDialog by remember { mutableStateOf(false) }
    var contactToReset by remember { mutableStateOf<Contact?>(null) }
    var activeContactActions by remember { mutableStateOf<Contact?>(null) }

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
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
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

    if (activeContactActions != null) {
        val activeContact = activeContactActions!!
        AlertDialog(
            onDismissRequest = { activeContactActions = null },
            title = {
                Text(
                    text = activeContact.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.contact_actions_message),
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (activeContact.score > 0) {
                        TextButton(
                            onClick = {
                                contactToReset = activeContact
                                activeContactActions = null
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.reset),
                                color = AccentColor,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            onRescanContact(activeContact.id)
                            activeContactActions = null
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.rescan_confirm_btn),
                            color = TextSecondary,
                        )
                    }
                    TextButton(
                        onClick = { activeContactActions = null },
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = AccentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp),
            )

            SortChip(
                text = stringResource(R.string.sort_score),
                selected = sortBy == ContactSortOrder.SCORE,
                ascending = sortAscending,
                onClick = {
                    if (sortBy == ContactSortOrder.SCORE) {
                        sortAscending = !sortAscending
                    } else {
                        sortBy = ContactSortOrder.SCORE
                        sortAscending = false
                    }
                },
            )

            Spacer(modifier = Modifier.width(8.dp))

            SortChip(
                text = stringResource(R.string.sort_name),
                selected = sortBy == ContactSortOrder.NAME,
                ascending = sortAscending,
                onClick = {
                    if (sortBy == ContactSortOrder.NAME) {
                        sortAscending = !sortAscending
                    } else {
                        sortBy = ContactSortOrder.NAME
                        sortAscending = true
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sortedAndFilteredContacts.isEmpty()) {
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
                items(sortedAndFilteredContacts) { contact ->
                    ContactCard(
                        contact = contact,
                        ringtones = ringtones,
                        isPlaying = contact.currentRingtone != null && playingUri == contact.currentRingtone,
                        onTogglePlay = {
                            contact.currentRingtone?.let { onTogglePlay(it) }
                        },
                        onClick = { activeContactActions = contact },
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
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
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
                }
            }
        }
    }
}

@Composable
fun SortChip(
    text: String,
    selected: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (selected) AccentColor.copy(alpha = 0.15f) else CardBackground,
        border =
            BorderStroke(
                width = 1.dp,
                color = if (selected) AccentColor else BorderColor,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = if (selected) AccentColor else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
            )
            if (selected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
