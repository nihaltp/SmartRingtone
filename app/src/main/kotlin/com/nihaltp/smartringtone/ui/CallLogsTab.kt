package com.nihaltp.smartringtone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihaltp.smartringtone.R
import com.nihaltp.smartringtone.data.CallLogEntry
import java.text.SimpleDateFormat
import java.util.*

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
