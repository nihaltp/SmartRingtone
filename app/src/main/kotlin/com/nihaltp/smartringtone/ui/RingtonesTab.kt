package com.nihaltp.smartringtone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nihaltp.smartringtone.data.Ringtone

@Composable
fun RingtonesTab(
    ringtones: List<Ringtone>,
    playingUri: String?,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onMove: (Int, Boolean) -> Unit,
    onTogglePlay: (String) -> Unit,
) {
    var ringtoneToDelete by remember { mutableStateOf<Ringtone?>(null) }

    if (ringtoneToDelete != null) {
        val ringtone = ringtoneToDelete!!
        AlertDialog(
            onDismissRequest = { ringtoneToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_ringtone_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_ringtone_confirm_message, ringtone.name),
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(ringtone.id)
                        ringtoneToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete_btn),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { ringtoneToDelete = null }) {
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
                        onDelete = { ringtoneToDelete = ringtone },
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
