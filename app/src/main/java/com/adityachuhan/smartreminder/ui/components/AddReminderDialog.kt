package com.adityachuhan.smartreminder.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adityachuhan.smartreminder.data.Reminder
import com.adityachuhan.smartreminder.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddReminderDialog(
    initialReminder: Reminder? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, triggerAt: Long, repeat: String, repeatDayOfWeek: Int?) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var note by remember { mutableStateOf(initialReminder?.note ?: "") }
    var repeat by remember { mutableStateOf(initialReminder?.repeat ?: "NONE") }

    val defaultTime = remember(initialReminder) {
        if (initialReminder != null) {
            initialReminder.triggerAt
        } else {
            Calendar.getInstance().apply {
                add(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
    var selectedTimeMillis by remember { mutableLongStateOf(defaultTime) }

    val calInit = remember(selectedTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
    }
    var selectedDayOfWeek by remember {
        mutableStateOf(initialReminder?.repeatDayOfWeek ?: calInit.get(Calendar.DAY_OF_WEEK))
    }

    val context = LocalContext.current
    val dateTimeFormatter = remember {
        SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
    }

    val daysOfWeek = listOf(
        2 to "Mon",
        3 to "Tue",
        4 to "Wed",
        5 to "Thu",
        6 to "Fri",
        7 to "Sat",
        1 to "Sun"
    )

    fun showPickers() {
        val base = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                base.set(Calendar.YEAR, year)
                base.set(Calendar.MONTH, month)
                base.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        base.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        base.set(Calendar.MINUTE, minute)
                        base.set(Calendar.SECOND, 0)
                        base.set(Calendar.MILLISECOND, 0)
                        selectedTimeMillis = base.timeInMillis
                        selectedDayOfWeek = base.get(Calendar.DAY_OF_WEEK)
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    false
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun applyPreset(minutesFromNow: Int) {
        val c = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutesFromNow)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedTimeMillis = c.timeInMillis
        selectedDayOfWeek = c.get(Calendar.DAY_OF_WEEK)
    }

    fun applyTomorrowMorning() {
        val c = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedTimeMillis = c.timeInMillis
        selectedDayOfWeek = c.get(Calendar.DAY_OF_WEEK)
    }

    fun applyTonight() {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        selectedTimeMillis = c.timeInMillis
        selectedDayOfWeek = c.get(Calendar.DAY_OF_WEEK)
    }

    val suggestionTitles = listOf("Assignment", "Lecture", "Study Session", "Medicine", "Workout", "Meeting")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = if (initialReminder == null) "New Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (initialReminder == null) "Reliable alarm precision even in Doze sleep" else "Update scheduled time and repeat options",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder title *") },
                    placeholder = { Text("e.g. Physics Lab, Assignment") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick suggestions
                if (title.isBlank()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestionTitles.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { title = suggestion },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Room, links, description...") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Time Picker Display Button
                Text(
                    text = "Scheduled Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = ::showPickers,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = dateTimeFormatter.format(Date(selectedTimeMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionChip(
                        onClick = { applyPreset(15) },
                        label = { Text("+15m") }
                    )
                    SuggestionChip(
                        onClick = { applyPreset(60) },
                        label = { Text("+1h") }
                    )
                    SuggestionChip(
                        onClick = { applyTonight() },
                        label = { Text("Tonight 8 PM") }
                    )
                    SuggestionChip(
                        onClick = { applyTomorrowMorning() },
                        label = { Text("Tomorrow 9 AM") }
                    )
                }

                // Repeat Schedule Options
                Text(
                    text = "Repeat Schedule",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val repeatOptions = listOf(
                        "NONE" to "One-time",
                        "DAILY" to "Daily",
                        "WEEKDAYS" to "Weekdays (Mon-Fri)",
                        "WEEKLY" to "Weekly"
                    )
                    repeatOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = repeat == value,
                            onClick = { repeat = value },
                            label = { Text(label, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // If Weekly: Day of week picker
                if (repeat == "WEEKLY") {
                    Text(
                        text = "Repeats on:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        daysOfWeek.forEach { (dayNum, dayLabel) ->
                            val isSelected = selectedDayOfWeek == dayNum
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDayOfWeek = dayNum },
                                label = { Text(dayLabel, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val repeatDay = if (repeat == "WEEKLY") selectedDayOfWeek else null
                        onSave(title.trim(), note.trim(), selectedTimeMillis, repeat, repeatDay)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text(
                    text = if (initialReminder == null) "Save Reminder" else "Update Reminder",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
