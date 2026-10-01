package com.adityachuhan.smartreminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityachuhan.smartreminder.data.Reminder
import com.adityachuhan.smartreminder.data.TodoItem
import com.adityachuhan.smartreminder.ui.components.*
import com.adityachuhan.smartreminder.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("reminders", "Reminders & Classes", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High-priority Smart Reminder and Timetable alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SmartReminderTheme {
                MainAppScaffold()
            }
        }
    }
}

enum class MainTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    REMINDERS("Reminders", Icons.Default.NotificationsActive),
    TODOS("To-Do", Icons.Default.Checklist),
    TIMETABLE("Timetable", Icons.Default.School),
    SETTINGS("Settings", Icons.Default.Settings)
}

enum class FilterOption(val label: String) {
    ALL("All"),
    TODAY("Today"),
    ACTIVE("Active"),
    REPEATING("Repeating"),
    ONE_TIME("One-time")
}

enum class TodoFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(
    vm: ReminderViewModel = viewModel(),
    timetableVm: TimetableViewModel = viewModel(),
    todoVm: TodoViewModel = viewModel()
) {
    val context = LocalContext.current
    val reminders by vm.reminders.collectAsState()
    val todos by todoVm.todos.collectAsState()
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(MainTab.REMINDERS) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }

    // To-Do Dialog States
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var todoToEdit by remember { mutableStateOf<TodoItem?>(null) }
    var todoToDelete by remember { mutableStateOf<TodoItem?>(null) }

    // Timetable screenshot state
    var entries by remember { mutableStateOf<List<TimetableEntry>>(emptyList()) }
    var reading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activeCount = remember(reminders) { reminders.count { it.enabled } }
    val pendingTodoCount = remember(todos) { todos.count { !it.isCompleted } }

    fun processScreenshot(uri: Uri) {
        reading = true
        scope.launch {
            try {
                val found = TimetableOcr.extract(context, uri)
                entries = found
                if (found.isEmpty()) {
                    error = "No classes detected. Please use a clear timetable screenshot with weekday names and time columns."
                }
            } catch (e: Exception) {
                error = "Could not read this image: ${e.localizedMessage ?: "Unknown error"}. Please choose a standard JPG or PNG timetable image."
            } finally {
                reading = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::processScreenshot)
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::processScreenshot)
    }

    fun chooseScreenshot() {
        if (Build.VERSION.SDK_INT >= 33) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IndigoPrimary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = when (currentTab) {
                                    MainTab.REMINDERS -> Icons.Default.Alarm
                                    MainTab.TODOS -> Icons.Default.Checklist
                                    MainTab.TIMETABLE -> Icons.Default.School
                                    MainTab.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = when (currentTab) {
                                    MainTab.REMINDERS -> "Smart Reminder"
                                    MainTab.TODOS -> "To-Do List"
                                    MainTab.TIMETABLE -> "Weekly Timetable"
                                    MainTab.SETTINGS -> "Settings"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Offline • Alarm-grade",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (currentTab == MainTab.REMINDERS || currentTab == MainTab.TIMETABLE) {
                        IconButton(onClick = ::chooseScreenshot) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Scan Timetable Screenshot",
                                tint = PurpleSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            when {
                                tab == MainTab.REMINDERS && activeCount > 0 -> {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = IndigoPrimary,
                                                contentColor = Color.White
                                            ) {
                                                Text(activeCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                }
                                tab == MainTab.TODOS && pendingTodoCount > 0 -> {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = PurpleSecondary,
                                                contentColor = Color.White
                                            ) {
                                                Text(pendingTodoCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                }
                                else -> {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            indicatorColor = IndigoContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            when (currentTab) {
                MainTab.REMINDERS, MainTab.TIMETABLE -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            reminderToEdit = null
                            showAddReminderDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("New reminder", fontWeight = FontWeight.Bold) },
                        containerColor = IndigoPrimary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    )
                }
                MainTab.TODOS -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            todoToEdit = null
                            showAddTodoDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("New task", fontWeight = FontWeight.Bold) },
                        containerColor = PurpleSecondary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    )
                }
                MainTab.SETTINGS -> {}
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                MainTab.REMINDERS -> {
                    RemindersTabContent(
                        reminders = reminders,
                        vm = vm,
                        onScanClick = ::chooseScreenshot,
                        onAddClick = {
                            reminderToEdit = null
                            showAddReminderDialog = true
                        },
                        onEditClick = { reminder ->
                            reminderToEdit = reminder
                            showAddReminderDialog = true
                        },
                        onDeleteClick = { reminder ->
                            reminderToDelete = reminder
                        },
                        onSnooze = { reminder, mins ->
                            vm.snooze(reminder, mins)
                            scope.launch {
                                snackbarHostState.showSnackbar("Snoozed for ${if (mins < 60) "${mins}m" else "1 hour"}")
                            }
                        }
                    )
                }
                MainTab.TODOS -> {
                    TodoTabContent(
                        todos = todos,
                        todoVm = todoVm,
                        onAddClick = {
                            todoToEdit = null
                            showAddTodoDialog = true
                        },
                        onEditClick = { todo ->
                            todoToEdit = todo
                            showAddTodoDialog = true
                        },
                        onDeleteClick = { todo ->
                            todoToDelete = todo
                        },
                        onScheduleReminderForTodo = { todo ->
                            reminderToEdit = Reminder(
                                title = todo.title,
                                note = if (todo.note.isNotBlank()) todo.note else "Task from To-Do: ${todo.category}",
                                triggerAt = todo.dueDate ?: (System.currentTimeMillis() + 60 * 60 * 1000L),
                                repeat = "NONE"
                            )
                            showAddReminderDialog = true
                        }
                    )
                }
                MainTab.TIMETABLE -> {
                    TimetableTabContent(
                        reminders = reminders,
                        vm = vm,
                        onScanClick = ::chooseScreenshot,
                        onEditClick = { reminder ->
                            reminderToEdit = reminder
                            showAddReminderDialog = true
                        },
                        onDeleteClick = { reminder ->
                            reminderToDelete = reminder
                        }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsTabContent(
                        totalReminders = reminders.size,
                        activeReminders = activeCount,
                        totalTodos = todos.size,
                        pendingTodos = pendingTodoCount,
                        onTriggerTest = {
                            val alarm = context.getSystemService(AlarmManager::class.java)
                            val intent = Intent(context, ReminderReceiver::class.java).apply {
                                putExtra("id", 88888L)
                                putExtra("title", "Smart Reminder: Alert Test Successful!")
                                putExtra("note", "Your sound, vibration, and exact alarm delivery are functioning properly.")
                                putExtra("repeat", "NONE")
                            }
                            val pending = PendingIntent.getBroadcast(
                                context,
                                88888,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val triggerTime = System.currentTimeMillis() + 4000L
                            if (Build.VERSION.SDK_INT >= 31 && alarm != null && alarm.canScheduleExactAlarms()) {
                                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
                            } else {
                                alarm?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("Test alarm scheduled! It will fire in 4 seconds.")
                            }
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Reminder Dialog
    if (showAddReminderDialog) {
        AddReminderDialog(
            initialReminder = reminderToEdit,
            onDismiss = {
                showAddReminderDialog = false
                reminderToEdit = null
            },
            onSave = { title, note, time, repeat, repeatDayOfWeek ->
                if (reminderToEdit != null && reminderToEdit!!.id > 0) {
                    val updated = reminderToEdit!!.copy(
                        title = title,
                        note = note,
                        triggerAt = time,
                        repeat = repeat,
                        repeatDayOfWeek = repeatDayOfWeek,
                        enabled = true
                    )
                    vm.update(updated)
                    scope.launch { snackbarHostState.showSnackbar("Reminder updated") }
                } else {
                    vm.add(title, note, time, repeat, repeatDayOfWeek)
                    scope.launch { snackbarHostState.showSnackbar("Reminder scheduled") }
                }
                showAddReminderDialog = false
                reminderToEdit = null
            }
        )
    }

    // Add / Edit Todo Dialog
    if (showAddTodoDialog) {
        AddEditTodoDialog(
            initialTodo = todoToEdit,
            onDismiss = {
                showAddTodoDialog = false
                todoToEdit = null
            },
            onSave = { title, note, priority, dueDate, category ->
                if (todoToEdit != null) {
                    val updated = todoToEdit!!.copy(
                        title = title,
                        note = note,
                        priority = priority,
                        dueDate = dueDate,
                        category = category
                    )
                    todoVm.update(updated)
                    scope.launch { snackbarHostState.showSnackbar("Task updated") }
                } else {
                    todoVm.add(title, note, priority, dueDate, category)
                    scope.launch { snackbarHostState.showSnackbar("Task added to list") }
                }
                showAddTodoDialog = false
                todoToEdit = null
            }
        )
    }

    // Delete Reminder Confirmation Dialog
    reminderToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Delete Reminder?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${target.title}\"? Scheduled alarms will be cancelled immediately.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.delete(target)
                        reminderToDelete = null
                        scope.launch { snackbarHostState.showSnackbar("Reminder deleted") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Todo Confirmation Dialog
    todoToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { todoToDelete = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Delete Task?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${target.title}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        todoVm.delete(target)
                        todoToDelete = null
                        scope.launch { snackbarHostState.showSnackbar("Task deleted") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { todoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Timetable Detected Dialog
    if (entries.isNotEmpty()) {
        TimetableImportDialog(
            entries = entries,
            onDismiss = { entries = emptyList() },
            onImport = { leadMinutes, selectedEntries ->
                timetableVm.importEntries(selectedEntries, leadMinutes) {
                    val count = selectedEntries.size
                    entries = emptyList()
                    scope.launch {
                        snackbarHostState.showSnackbar("Scheduled $count weekly class reminders!")
                    }
                }
            }
        )
    }

    // Scanning OCR Indicator Dialog
    if (reading) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reading Timetable", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = IndigoPrimary
                    )
                    Text(
                        text = "Analyzing days, time slots, and subject names with on-device ML Kit OCR...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Error Dialog
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Timetable Scan", fontWeight = FontWeight.Black) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { error = null }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun RemindersTabContent(
    reminders: List<Reminder>,
    vm: ReminderViewModel,
    onScanClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Reminder) -> Unit,
    onDeleteClick: (Reminder) -> Unit,
    onSnooze: (Reminder, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FilterOption.ALL) }

    val calNow = Calendar.getInstance()
    val todayRemindersCount = remember(reminders) {
        reminders.count { r ->
            val c = Calendar.getInstance().apply { timeInMillis = r.triggerAt }
            c.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    c.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) &&
                    r.enabled
        }
    }

    val activeCount = remember(reminders) { reminders.count { it.enabled } }
    val repeatingCount = remember(reminders) { reminders.count { it.repeat != "NONE" && it.enabled } }

    val filteredList = remember(reminders, searchQuery, selectedFilter) {
        reminders.filter { r ->
            val matchesQuery = searchQuery.isBlank() ||
                    r.title.contains(searchQuery, ignoreCase = true) ||
                    r.note.contains(searchQuery, ignoreCase = true)

            if (!matchesQuery) return@filter false

            when (selectedFilter) {
                FilterOption.ALL -> true
                FilterOption.TODAY -> {
                    val c = Calendar.getInstance().apply { timeInMillis = r.triggerAt }
                    c.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                            c.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                }
                FilterOption.ACTIVE -> r.enabled
                FilterOption.REPEATING -> r.repeat != "NONE"
                FilterOption.ONE_TIME -> r.repeat == "NONE"
            }
        }.sortedWith(
            compareBy<Reminder> { !it.enabled }
                .thenBy { it.triggerAt }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
        item {
            HeroCard(
                activeCount = activeCount,
                todayCount = todayRemindersCount,
                onUploadClick = onScanClick,
                onAddClick = onAddClick
            )
        }

        // Quick Stat Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    count = activeCount.toString(),
                    label = "Active",
                    icon = Icons.Default.NotificationsActive,
                    iconTint = IndigoPrimary,
                    isSelected = selectedFilter == FilterOption.ACTIVE,
                    onClick = {
                        selectedFilter = if (selectedFilter == FilterOption.ACTIVE) FilterOption.ALL else FilterOption.ACTIVE
                    },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    count = todayRemindersCount.toString(),
                    label = "Today",
                    icon = Icons.Default.CalendarToday,
                    iconTint = CyanTertiary,
                    isSelected = selectedFilter == FilterOption.TODAY,
                    onClick = {
                        selectedFilter = if (selectedFilter == FilterOption.TODAY) FilterOption.ALL else FilterOption.TODAY
                    },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    count = repeatingCount.toString(),
                    label = "Repeating",
                    icon = Icons.Default.Repeat,
                    iconTint = PurpleSecondary,
                    isSelected = selectedFilter == FilterOption.REPEATING,
                    onClick = {
                        selectedFilter = if (selectedFilter == FilterOption.REPEATING) FilterOption.ALL else FilterOption.REPEATING
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search reminders, notes, classes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FilterOption.entries) { option ->
                    val isSelected = selectedFilter == option
                    val count = when (option) {
                        FilterOption.ALL -> reminders.size
                        FilterOption.TODAY -> todayRemindersCount
                        FilterOption.ACTIVE -> activeCount
                        FilterOption.REPEATING -> repeatingCount
                        FilterOption.ONE_TIME -> reminders.count { it.repeat == "NONE" }
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = option },
                        label = {
                            Text(
                                text = "${option.label} ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedFilter.label} Reminders (${filteredList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Reminder List or Empty State
        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IndigoContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching reminders" else "No reminders scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try a different search keyword."
                            } else {
                                "Tap 'New reminder' or scan your timetable screenshot to get started."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAddClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Text("New reminder")
                            }
                            OutlinedButton(
                                onClick = onScanClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Scan timetable")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggleEnable = { enabled -> vm.update(reminder.copy(enabled = enabled)) },
                    onEdit = { onEditClick(reminder) },
                    onDelete = { onDeleteClick(reminder) },
                    onSnooze = { mins -> onSnooze(reminder, mins) }
                )
            }
        }
    }
}

@Composable
private fun TodoTabContent(
    todos: List<TodoItem>,
    todoVm: TodoViewModel,
    onAddClick: () -> Unit,
    onEditClick: (TodoItem) -> Unit,
    onDeleteClick: (TodoItem) -> Unit,
    onScheduleReminderForTodo: (TodoItem) -> Unit
) {
    var quickTaskText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TodoFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val totalCount = todos.size
    val completedCount = remember(todos) { todos.count { it.isCompleted } }
    val pendingCount = totalCount - completedCount
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    val categories = remember(todos) {
        todos.map { it.category }.distinct().sorted()
    }

    val filteredTodos = remember(todos, selectedFilter, selectedCategory) {
        todos.filter { t ->
            val matchesFilter = when (selectedFilter) {
                TodoFilter.ALL -> true
                TodoFilter.PENDING -> !t.isCompleted
                TodoFilter.HIGH_PRIORITY -> t.priority == "HIGH" && !t.isCompleted
                TodoFilter.COMPLETED -> t.isCompleted
            }
            val matchesCategory = selectedCategory == null || t.category == selectedCategory
            matchesFilter && matchesCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "To-Do & Tasks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Track your assignments, errands, and daily checklist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress Card
        if (totalCount > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Task Completion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (progress >= 1f) SuccessGreenContainer else PurpleContainer
                            ) {
                                Text(
                                    text = "$completedCount of $totalCount (${(progress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progress >= 1f) SuccessGreen else PurpleOnContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (progress >= 1f) SuccessGreen else PurpleSecondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Add Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = quickTaskText,
                        onValueChange = { quickTaskText = it },
                        placeholder = { Text("Quickly add a task...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (quickTaskText.isNotBlank()) {
                                todoVm.add(quickTaskText.trim())
                                quickTaskText = ""
                            }
                        },
                        enabled = quickTaskText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TodoFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        TodoFilter.ALL -> totalCount
                        TodoFilter.PENDING -> pendingCount
                        TodoFilter.HIGH_PRIORITY -> todos.count { it.priority == "HIGH" && !it.isCompleted }
                        TodoFilter.COMPLETED -> completedCount
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = "${filter.label} ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Category Filter Chips Row if categories exist
        if (categories.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Categories") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Section Title & Clear Completed Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedFilter.label} Tasks (${filteredTodos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (completedCount > 0 && selectedFilter != TodoFilter.PENDING) {
                    TextButton(
                        onClick = { todoVm.clearCompleted() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Task Items or Empty State
        if (filteredTodos.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedFilter == TodoFilter.COMPLETED) Icons.Default.CheckCircle else Icons.Default.PlaylistAddCheck,
                                    contentDescription = null,
                                    tint = PurpleSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = if (selectedFilter == TodoFilter.COMPLETED) {
                                "No completed tasks yet"
                            } else if (totalCount > 0 && pendingCount == 0) {
                                "All tasks completed! 🎉"
                            } else {
                                "No tasks found"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (totalCount == 0) {
                                "Add your first task above or tap 'New task'."
                            } else {
                                "Check off items as you finish them or switch filters."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTodos, key = { it.id }) { todo ->
                TodoItemCard(
                    todo = todo,
                    onToggleCompleted = { todoVm.toggleCompleted(todo) },
                    onEdit = { onEditClick(todo) },
                    onDelete = { onDeleteClick(todo) },
                    onScheduleReminder = { onScheduleReminderForTodo(todo) }
                )
            }
        }
    }
}

@Composable
private fun TimetableTabContent(
    reminders: List<Reminder>,
    vm: ReminderViewModel,
    onScanClick: () -> Unit,
    onEditClick: (Reminder) -> Unit,
    onDeleteClick: (Reminder) -> Unit
) {
    var selectedDayNumber by remember {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        mutableIntStateOf(today) // 1=Sun, 2=Mon... 7=Sat
    }

    val days = listOf(
        2 to "Mon",
        3 to "Tue",
        4 to "Wed",
        5 to "Thu",
        6 to "Fri",
        7 to "Sat",
        1 to "Sun"
    )

    // Precalculate counts for each day
    val dayCounts = remember(reminders) {
        days.associate { (dayNum, _) ->
            dayNum to reminders.count { r ->
                if (!r.enabled) return@count false
                if (r.repeat == "DAILY") return@count true
                if (r.repeat == "WEEKDAYS" && dayNum in 2..6) return@count true
                if (r.repeat == "WEEKLY") {
                    val dayOfWeek = r.repeatDayOfWeek ?: Calendar.getInstance().apply { timeInMillis = r.triggerAt }.get(Calendar.DAY_OF_WEEK)
                    return@count dayOfWeek == dayNum
                }
                false
            }
        }
    }

    // Filter reminders that repeat on this day
    val classesForDay = remember(reminders, selectedDayNumber) {
        reminders.filter { r ->
            if (r.repeat == "DAILY") return@filter true
            if (r.repeat == "WEEKDAYS" && selectedDayNumber in 2..6) return@filter true
            if (r.repeat == "WEEKLY") {
                val dayOfWeek = r.repeatDayOfWeek ?: Calendar.getInstance().apply { timeInMillis = r.triggerAt }.get(Calendar.DAY_OF_WEEK)
                return@filter dayOfWeek == selectedDayNumber
            }
            false
        }.sortedBy { r ->
            val c = Calendar.getInstance().apply { timeInMillis = r.triggerAt }
            c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Weekly Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Recurring classes, lectures, and timetable routine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Scan CTA Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Timetable Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OCR detects days, times, and subject names",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onScanClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Day of Week Tabs with Counts
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days) { (dayNum, name) ->
                    val isSelected = selectedDayNumber == dayNum
                    val count = dayCounts[dayNum] ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDayNumber = dayNum },
                        label = {
                            Text(
                                text = "$name • $count",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Classes on this day (${classesForDay.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (classesForDay.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = PurpleSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No classes on this day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload a timetable image to automatically populate your week or create custom reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(classesForDay, key = { it.id }) { cls ->
                ReminderCard(
                    reminder = cls,
                    onToggleEnable = { enabled -> vm.update(cls.copy(enabled = enabled)) },
                    onEdit = { onEditClick(cls) },
                    onDelete = { onDeleteClick(cls) },
                    onSnooze = { mins -> vm.snooze(cls, mins) }
                )
            }
        }
    }
}

@Composable
private fun SettingsTabContent(
    totalReminders: Int,
    activeReminders: Int,
    totalTodos: Int,
    pendingTodos: Int,
    onTriggerTest: () -> Unit
) {
    val context = LocalContext.current
    val notificationsAllowed = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val exactAlarmsAllowed = remember {
        if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "System reliability and diagnostics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Notification Permissions Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = IndigoPrimary)
                            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (notificationsAllowed) SuccessGreenContainer else DangerRedContainer
                        ) {
                            Text(
                                text = if (notificationsAllowed) "Granted" else "Disabled",
                                color = if (notificationsAllowed) SuccessGreen else DangerRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Allows Smart Reminder to alert you with high-priority heads-up banners when reminders and classes arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!notificationsAllowed) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enable in System Settings")
                        }
                    }
                }
            }
        }

        // Exact Alarm Permission Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Alarm, null, tint = PurpleSecondary)
                            Text("Exact Alarms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (exactAlarmsAllowed) SuccessGreenContainer else WarningAmberContainer
                        ) {
                            Text(
                                text = if (exactAlarmsAllowed) "Exact (Doze-safe)" else "Inexact",
                                color = if (exactAlarmsAllowed) SuccessGreen else WarningAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Enables precise alarm delivery even while your phone is asleep in deep Doze mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (Build.VERSION.SDK_INT >= 31 && !exactAlarmsAllowed) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant Exact Alarm Permission")
                        }
                    }
                }
            }
        }

        // Test Notification Diagnostics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = CyanTertiary)
                        Text("Alarm Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Trigger a 4-second test alarm to verify your phone's notification channel, volume, and vibration work properly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onTriggerTest,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanTertiary)
                    ) {
                        Icon(Icons.Default.Alarm, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Alarm (Fires in 4s)")
                    }
                }
            }
        }

        // Privacy & Architecture Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, null, tint = SuccessGreen)
                        Text("100% Offline & Private", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "• No accounts or cloud sync required\n• Zero analytics, advertising, or telemetry SDKs\n• On-device Google ML Kit OCR text processing\n• Local SQLite Room database with reactive StateFlow\n• Automatic reboot recovery via AlarmManager\n• Currently tracking $totalReminders reminders ($activeReminders active) & $totalTodos tasks ($pendingTodos pending)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
