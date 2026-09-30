package com.example.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TimetableClass
import com.example.timetable.ParsedClassPreview
import com.example.timetable.TimetableParser
import com.example.ui.common.DateTimeUtils
import com.example.ui.common.StatusBadge
import com.example.ui.viewmodel.TimetableViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val classes by viewModel.classesForSelectedDay.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<TimetableClass?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val dayNames = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_class")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with Import button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Class Schedule",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recurring class reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("timetable_import_button")
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import")
                }
            }

            // Day Selector Tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedDay - 1,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                dayNames.forEach { (dayNumber, label) ->
                    Tab(
                        selected = selectedDay == dayNumber,
                        onClick = { viewModel.setSelectedDay(dayNumber) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedDay == dayNumber) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Class list for selected day
            if (classes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No classes on this day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+' to add a class or 'Import' to paste a schedule",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = classes, key = { it.id }) { cls ->
                        ClassCard(
                            timetableClass = cls,
                            onToggleEnabled = { viewModel.toggleClassEnabled(cls) },
                            onEdit = { editingClass = cls },
                            onDelete = { viewModel.deleteClass(cls) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Class Dialog
    if (showAddDialog || editingClass != null) {
        AddEditClassDialog(
            initialClass = editingClass,
            defaultDay = selectedDay,
            onDismiss = {
                showAddDialog = false
                editingClass = null
            },
            onSave = { classToSave ->
                viewModel.saveClass(classToSave)
                showAddDialog = false
                editingClass = null
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        ImportTimetableDialog(
            onDismiss = { showImportDialog = false },
            onImport = { importedClasses ->
                viewModel.importClasses(importedClasses)
                showImportDialog = false
            }
        )
    }
}

@Composable
fun ClassCard(
    timetableClass: TimetableClass,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timetableClass.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (timetableClass.room.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = timetableClass.room,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (timetableClass.teacher.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = timetableClass.teacher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = timetableClass.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = timetableClass.getFormattedTimeRange(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (timetableClass.advanceMinutes > 0) {
                        StatusBadge(
                            text = "${timetableClass.advanceMinutes}m before",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Class", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Class",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditClassDialog(
    initialClass: TimetableClass? = null,
    defaultDay: Int = 1,
    onDismiss: () -> Unit,
    onSave: (TimetableClass) -> Unit
) {
    var subject by remember { mutableStateOf(initialClass?.subject ?: "") }
    var teacher by remember { mutableStateOf(initialClass?.teacher ?: "") }
    var room by remember { mutableStateOf(initialClass?.room ?: "") }

    var startHour by remember { mutableIntStateOf(initialClass?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initialClass?.startMinute ?: 30) }
    var endHour by remember { mutableIntStateOf(initialClass?.endHour ?: 10) }
    var endMinute by remember { mutableIntStateOf(initialClass?.endMinute ?: 30) }

    var advanceMinutes by remember { mutableIntStateOf(initialClass?.advanceMinutes ?: 5) }
    var isEnabled by remember { mutableStateOf(initialClass?.isEnabled ?: true) }

    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (initialClass != null) {
                addAll(initialClass.getDaysList())
            } else {
                add(defaultDay)
            }
        }
    }

    var subjectError by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
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
                    text = if (initialClass == null) "New Class" else "Edit Class",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        if (it.isNotBlank()) subjectError = false
                    },
                    label = { Text("Subject *") },
                    placeholder = { Text("e.g. Business Studies") },
                    isError = subjectError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room") },
                        placeholder = { Text("e.g. 204") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher") },
                        placeholder = { Text("e.g. Mr. Smith") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Time") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Time") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week selector
                Text(
                    text = "Days of Week:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                val days = listOf(
                    1 to "Mon",
                    2 to "Tue",
                    3 to "Wed",
                    4 to "Thu",
                    5 to "Fri",
                    6 to "Sat",
                    7 to "Sun"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    days.forEach { (dayInt, label) ->
                        val isSelected = selectedDays.contains(dayInt)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays.remove(dayInt)
                                } else {
                                    selectedDays.add(dayInt)
                                }
                            },
                            label = { Text(label) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Advance Notification Dropdown
                val advanceOptions = listOf(
                    0 to "At class time",
                    5 to "5 minutes before",
                    10 to "10 minutes before",
                    15 to "15 minutes before",
                    30 to "30 minutes before"
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
                        label = { Text("Notify me") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = advanceDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subject.isBlank()) {
                                subjectError = true
                                return@Button
                            }
                            val cls = TimetableClass(
                                id = initialClass?.id ?: 0L,
                                subject = subject.trim(),
                                teacher = teacher.trim(),
                                room = room.trim(),
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                daysOfWeek = selectedDays.sorted().joinToString(","),
                                advanceMinutes = advanceMinutes,
                                isEnabled = isEnabled,
                                notificationId = initialClass?.notificationId ?: ((System.currentTimeMillis() % 1000000).toInt() + 10000),
                                createdTimestamp = initialClass?.createdTimestamp ?: System.currentTimeMillis()
                            )
                            onSave(cls)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Class")
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    startHour = timePickerState.hour
                    startMinute = timePickerState.minute
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select End Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    endHour = timePickerState.hour
                    endMinute = timePickerState.minute
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ImportTimetableDialog(
    onDismiss: () -> Unit,
    onImport: (List<TimetableClass>) -> Unit
) {
    var rawText by remember {
        mutableStateOf(
            """Monday
9:30 Business Studies Room 201
10:30 Economics Room 104
11:30 Accounting with Mr. Taylor

Tuesday
9:30 Management Room 201
10:30 Business Studies"""
        )
    }

    var parsedItems by remember { mutableStateOf(TimetableParser.parse(rawText)) }

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
                    text = "Import Timetable",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste your timetable text below. Day names and time ranges are automatically recognized.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        parsedItems = TimetableParser.parse(it)
                    },
                    label = { Text("Paste Schedule") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Parsed Classes Preview (${parsedItems.size} detected):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (parsedItems.isEmpty()) {
                    Text(
                        text = "No classes recognized yet. Example format:\nMonday\n9:30 Business Studies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    parsedItems.forEachIndexed { index, item ->
                        var isChecked by remember(item) { mutableStateOf(item.isSelected) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    isChecked = it
                                    item.isSelected = it
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.dayName}: ${item.subject}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.getFormattedTime()} ${if (item.room.isNotBlank()) "• ${item.room}" else ""} ${if (item.teacher.isNotBlank()) "• ${item.teacher}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val selected = parsedItems.filter { it.isSelected }.map { it.toTimetableClass() }
                            if (selected.isNotEmpty()) {
                                onImport(selected)
                            }
                        },
                        enabled = parsedItems.any { it.isSelected },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Schedule")
                    }
                }
            }
        }
    }
}
