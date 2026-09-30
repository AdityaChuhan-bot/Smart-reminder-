package com.example.ui.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Reminder
import com.example.data.model.RepeatType
import com.example.ui.common.DateTimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditReminderDialog(
    initialReminder: Reminder? = null,
    defaultAdvanceMinutes: Int = 0,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val initialDateTime = if (initialReminder != null) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(initialReminder.scheduledTimeMillis), zoneId)
    } else {
        // Default 1 hour from now, rounded to next 15 minutes
        val now = LocalDateTime.now().plusHours(1)
        now.withMinute((now.minute / 15) * 15).withSecond(0).withNano(0)
    }

    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialDateTime.toLocalTime()) }
    var repeatType by remember { mutableStateOf(initialReminder?.getRepeatTypeEnum() ?: RepeatType.NONE) }
    var advanceMinutes by remember { mutableIntStateOf(initialReminder?.advanceMinutes ?: defaultAdvanceMinutes) }
    var isEnabled by remember { mutableStateOf(initialReminder?.isEnabled ?: true) }

    // Custom days (1=Mon, 7=Sun)
    val customDays = remember {
        mutableStateListOf<Int>().apply {
            if (initialReminder != null && initialReminder.repeatDays.isNotBlank()) {
                addAll(initialReminder.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() })
            } else {
                add(initialDateTime.dayOfWeek.value)
            }
        }
    }

    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var repeatDropdownExpanded by remember { mutableStateOf(false) }
    var advanceDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialReminder == null) "New Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g., Attend Business Studies class") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add notes, link, or details...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_description_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date & Time Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Button
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                            .testTag("reminder_date_picker_trigger"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    // Time Button
                    OutlinedTextField(
                        value = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = "Pick Time")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker = true }
                            .testTag("reminder_time_picker_trigger"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat Dropdown
                ExposedDropdownMenuBox(
                    expanded = repeatDropdownExpanded,
                    onExpandedChange = { repeatDropdownExpanded = !repeatDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = repeatType.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repeat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("reminder_repeat_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = repeatDropdownExpanded,
                        onDismissRequest = { repeatDropdownExpanded = false }
                    ) {
                        RepeatType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.getDisplayName()) },
                                onClick = {
                                    repeatType = type
                                    repeatDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // If CUSTOM repeat, show day of week toggles
                if (repeatType == RepeatType.CUSTOM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Repeat on:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val daysLabels = listOf(
                        1 to "M",
                        2 to "T",
                        3 to "W",
                        4 to "T",
                        5 to "F",
                        6 to "S",
                        7 to "S"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        daysLabels.forEach { (dayInt, label) ->
                            val isSelected = customDays.contains(dayInt)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        if (customDays.size > 1) customDays.remove(dayInt)
                                    } else {
                                        customDays.add(dayInt)
                                    }
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Advance Notification Dropdown
                val advanceOptions = listOf(
                    0 to "At time",
                    5 to "5 minutes before",
                    10 to "10 minutes before",
                    15 to "15 minutes before",
                    30 to "30 minutes before",
                    60 to "1 hour before",
                    1440 to "1 day before"
                )

                ExposedDropdownMenuBox(
                    expanded = advanceDropdownExpanded,
                    onExpandedChange = { advanceDropdownExpanded = !advanceDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = advanceOptions.find { it.first == advanceMinutes }?.second ?: "${advanceMinutes}m before",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Remind me before") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = advanceDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("reminder_advance_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = advanceDropdownExpanded,
                        onDismissRequest = { advanceDropdownExpanded = false }
                    ) {
                        advanceOptions.forEach { (mins, text) ->
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    advanceMinutes = mins
                                    advanceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Enabled Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notification",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isEnabled) "Active alarm and alert" else "Muted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        modifier = Modifier.testTag("reminder_notification_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("reminder_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }

                            val scheduledLocalDateTime = LocalDateTime.of(selectedDate, selectedTime)
                            var scheduledMillis = scheduledLocalDateTime.atZone(zoneId).toInstant().toEpochMilli()

                            // If scheduled time is in past for a one-time reminder, push to tomorrow or warn
                            val nowMillis = System.currentTimeMillis()
                            if (repeatType == RepeatType.NONE && scheduledMillis <= nowMillis) {
                                // Auto advance to tomorrow same time if user picked an earlier time today
                                if (selectedDate == LocalDate.now(zoneId)) {
                                    val tomorrow = selectedDate.plusDays(1)
                                    scheduledMillis = LocalDateTime.of(tomorrow, selectedTime).atZone(zoneId).toInstant().toEpochMilli()
                                }
                            }

                            val reminderToSave = Reminder(
                                id = initialReminder?.id ?: 0L,
                                title = title.trim(),
                                description = description.trim(),
                                scheduledTimeMillis = scheduledMillis,
                                repeatType = repeatType.name,
                                repeatDays = customDays.sorted().joinToString(","),
                                isEnabled = isEnabled,
                                isCompleted = false,
                                notificationId = initialReminder?.notificationId ?: (System.currentTimeMillis() % 1000000).toInt(),
                                advanceMinutes = advanceMinutes,
                                createdTimestamp = initialReminder?.createdTimestamp ?: System.currentTimeMillis(),
                                updatedTimestamp = System.currentTimeMillis()
                            )
                            onSave(reminderToSave)
                        },
                        modifier = Modifier.testTag("reminder_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Reminder")
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
