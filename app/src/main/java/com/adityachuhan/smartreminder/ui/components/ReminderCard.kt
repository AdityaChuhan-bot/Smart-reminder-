package com.adityachuhan.smartreminder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adityachuhan.smartreminder.data.Reminder
import com.adityachuhan.smartreminder.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggleEnable: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isPast = reminder.triggerAt < now
    val isEnabled = reminder.enabled

    val calReminder = Calendar.getInstance().apply { timeInMillis = reminder.triggerAt }
    val calNow = Calendar.getInstance()

    val isToday = calReminder.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calReminder.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
    val isTomorrow = calReminder.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calReminder.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) + 1

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val formattedTime = timeFormat.format(Date(reminder.triggerAt))
    val formattedDate = dateFormat.format(Date(reminder.triggerAt))

    val scheduleText = when {
        isToday -> "Today • $formattedTime"
        isTomorrow -> "Tomorrow • $formattedTime"
        else -> "$formattedDate • $formattedTime"
    }

    // Relative countdown or overdue text
    val diffMillis = reminder.triggerAt - now
    val relativeText = when {
        isPast && reminder.repeat == "NONE" -> {
            val overdueMins = (now - reminder.triggerAt) / (60 * 1000)
            if (overdueMins < 60) "Overdue by ${overdueMins}m" else "Overdue by ${overdueMins / 60}h"
        }
        diffMillis in 0..(60 * 60 * 1000) -> {
            val mins = (diffMillis / (60 * 1000)).coerceAtLeast(1)
            "In $mins min${if (mins == 1L) "" else "s"}"
        }
        diffMillis in (60 * 60 * 1000)..(24 * 60 * 60 * 1000) -> {
            val hours = diffMillis / (60 * 60 * 1000)
            "In $hours hr${if (hours == 1L) "" else "s"}"
        }
        else -> null
    }

    val daysOfWeek = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val repeatText = when (reminder.repeat) {
        "DAILY" -> "Daily"
        "WEEKDAYS" -> "Mon - Fri"
        "WEEKLY" -> {
            val dayName = reminder.repeatDayOfWeek?.let { daysOfWeek.getOrNull(it) }
            if (dayName != null) "Every $dayName" else "Weekly"
        }
        else -> null
    }

    val isClass = reminder.title.startsWith("Class:", ignoreCase = true) ||
            reminder.note.contains("timetable", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isEnabled && isPast && reminder.repeat == "NONE") {
                    Modifier.border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                } else if (isEnabled && isToday) {
                    Modifier.border(1.dp, IndigoPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Schedule Badges and Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Timing Badge
                    val badgeColor = when {
                        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                        isPast && reminder.repeat == "NONE" -> DangerRed.copy(alpha = 0.12f)
                        isToday -> IndigoPrimary.copy(alpha = 0.12f)
                        else -> PurpleSecondary.copy(alpha = 0.12f)
                    }
                    val badgeTextColor = when {
                        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        isPast && reminder.repeat == "NONE" -> DangerRed
                        isToday -> IndigoPrimary
                        else -> PurpleSecondary
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPast && reminder.repeat == "NONE") Icons.Default.Alarm else Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = badgeTextColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = scheduleText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }

                    // Relative Badge (e.g. "In 25m" or "Overdue")
                    if (isEnabled && relativeText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPast) DangerRedContainer else CyanContainer
                        ) {
                            Text(
                                text = relativeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPast) DangerRed else CyanOnContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Repeating badge if applicable
                    if (repeatText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = PurpleOnContainer
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = repeatText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleOnContainer
                                )
                            }
                        }
                    }

                    // Class badge
                    if (isClass) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreenContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = SuccessGreen
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Class",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }

                // Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoPrimary
                    )
                )
            }

            // Title & Note
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (!isEnabled) TextDecoration.LineThrough else null,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (reminder.note.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reminder.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Snooze chips (visible if enabled)
                if (isEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSnooze(10) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "+10m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onSnooze(60) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "+1h",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Muted / Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Edit & Delete Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit reminder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete reminder",
                            tint = DangerRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
