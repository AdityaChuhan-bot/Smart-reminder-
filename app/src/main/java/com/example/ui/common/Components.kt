package com.example.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

    fun formatTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId)
        return dt.format(timeFormatter)
    }

    fun formatDate(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val today = LocalDate.now(zoneId)
        val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

        return when {
            date == today -> "Today"
            date == today.plusDays(1) -> "Tomorrow"
            date == today.minusDays(1) -> "Yesterday"
            date.year == today.year -> date.format(dateFormatter)
            else -> date.format(fullDateFormatter)
        }
    }

    fun formatDateTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return "${formatDate(millis, zoneId)}, ${formatTime(millis, zoneId)}"
    }

    fun formatAdvance(minutes: Int): String {
        return when (minutes) {
            0 -> "At time"
            5 -> "5m before"
            10 -> "10m before"
            15 -> "15m before"
            30 -> "30m before"
            60 -> "1h before"
            1440 -> "1d before"
            else -> "${minutes}m before"
        }
    }
}

@Composable
fun SnoozeDialog(
    reminderTitle: String,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Snooze,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Snooze Reminder", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column {
                Text(
                    text = "Snooze \"$reminderTitle\" for:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                val options = listOf(
                    5 to "5 Minutes",
                    10 to "10 Minutes",
                    15 to "15 Minutes",
                    30 to "30 Minutes",
                    60 to "1 Hour"
                )

                options.forEach { (mins, label) ->
                    OutlinedButton(
                        onClick = {
                            onSnooze(mins)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("snooze_${mins}m_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = label, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("snooze_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text("Enable Notifications", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                "Smart Reminder needs notification permission so it can alert you when your reminders and class schedules are due. Without this, notifications cannot be shown.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("permission_grant_button")
            ) {
                Text("Allow Notifications")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("permission_deny_button")
            ) {
                Text("Not Now")
            }
        }
    )
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
