package com.adityachuhan.smartreminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adityachuhan.smartreminder.ui.theme.CyanTertiary
import com.adityachuhan.smartreminder.ui.theme.IndigoPrimary
import com.adityachuhan.smartreminder.ui.theme.PurpleSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableImportDialog(
    entries: List<TimetableEntry>,
    onDismiss: () -> Unit,
    onImport: (leadMinutes: Int, selectedEntries: List<TimetableEntry>) -> Unit
) {
    var leadMinutes by remember { mutableIntStateOf(10) }
    val days = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val selectedMap = remember {
        mutableStateMapOf<TimetableEntry, Boolean>().apply {
            entries.forEach { this[it] = true }
        }
    }

    val selectedCount = selectedMap.values.count { it }
    val allSelected = selectedCount == entries.size

    fun formatClassTime(hour: Int, minute: Int): String {
        val ampm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, ampm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PurpleSecondary.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PurpleSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column {
                    Text("Timetable Detected", fontWeight = FontWeight.Black)
                    Text(
                        "$selectedCount of ${entries.size} classes selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = IndigoPrimary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Review extracted classes. Weekly recurring reminders will be set up automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lead time chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Remind me before each class:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            0 to "At class time",
                            5 to "5m before",
                            10 to "10m before",
                            15 to "15m before",
                            30 to "30m before"
                        ).forEach { (mins, label) ->
                            FilterChip(
                                selected = leadMinutes == mins,
                                onClick = { leadMinutes = mins },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Selection Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Classes (${entries.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            val newSelect = !allSelected
                            entries.forEach { selectedMap[it] = newSelect }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (allSelected) "Deselect All" else "Select All", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // List of detected classes
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries) { entry ->
                        val isChecked = selectedMap[entry] ?: true
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selectedMap[entry] = it }
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PurpleSecondary.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = days.getOrElse(entry.dayOfWeek) { "Day" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatClassTime(entry.hour, entry.minute),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = entries.filter { selectedMap[it] == true }
                    if (selected.isNotEmpty()) {
                        onImport(leadMinutes, selected)
                    }
                },
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Schedule $selectedCount classes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
