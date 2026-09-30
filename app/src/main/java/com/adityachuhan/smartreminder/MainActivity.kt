package com.adityachuhan.smartreminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityachuhan.smartreminder.data.Reminder
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val BrightBlue = Color(0xFF2563EB)
private val BrightCyan = Color(0xFF06B6D4)
private val BrightPurple = Color(0xFF7C3AED)
private val SoftBackground = Color(0xFFF5F7FF)

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = BrightBlue,
                    secondary = BrightPurple,
                    tertiary = BrightCyan,
                    background = SoftBackground,
                    surface = Color.White
                )
            ) { ReminderHome() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderHome(vm: ReminderViewModel = viewModel(), timetableVm: TimetableViewModel = viewModel()) {
    val context = LocalContext.current
    val reminders by vm.reminders.collectAsState()
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<TimetableEntry>>(emptyList()) }
    var reading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun process(uri: Uri) {
        reading = true
        scope.launch {
            try {
                val found = TimetableOcr.extract(context, uri)
                entries = found
                if (found.isEmpty()) error = "No classes detected. Use a clear timetable screenshot with weekday and time labels."
            } catch (_: Exception) {
                error = "Could not read this image. Please choose a JPG or PNG timetable screenshot."
            } finally { reading = false }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::process) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::process) }

    fun chooseScreenshot() {
        if (Build.VERSION.SDK_INT >= 33) {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            gallery.launch("image/*")
        }
    }

    Scaffold(
        containerColor = SoftBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Smart Reminder", fontWeight = FontWeight.Black)
                        Text("Plan it. Forget it. Get reminded.", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New reminder", fontWeight = FontWeight.Bold) },
                containerColor = BrightBlue,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HeroCard(reminders.count { it.enabled }, ::chooseScreenshot) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), Icons.Default.NotificationsActive, "\${reminders.count { it.enabled }}", "Active")
                    StatCard(Modifier.weight(1f), Icons.Default.CalendarMonth, "\${reminders.count { it.repeat != "NONE" }}", "Repeating")
                    StatCard(Modifier.weight(1f), Icons.Default.Schedule, "\${reminders.count { it.repeat == "NONE" }}", "One-time")
                }
            }
            item { Text("Upcoming reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (reminders.isEmpty()) {
                item { EmptyState(::chooseScreenshot) { showAdd = true } }
            } else {
                items(reminders, key = { it.id }) { ReminderCard(it, vm) }
            }
        }
    }

    if (showAdd) AddReminderDialog(
        onDismiss = { showAdd = false },
        onSave = { title, note, time, repeat -> vm.add(title, note, time, repeat); showAdd = false }
    )
    if (entries.isNotEmpty()) TimetableImportDialog(entries, { entries = emptyList() }) { lead ->
        timetableVm.importEntries(entries, lead) { entries = emptyList() }
    }
    if (reading) AlertDialog(
        onDismissRequest = {},
        title = { Text("Reading your timetable", fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Finding days, times and class names on your device.")
        } },
        confirmButton = {}
    )
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Import failed", fontWeight = FontWeight.Black) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun HeroCard(count: Int, onUpload: () -> Unit) {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(Color.Transparent)) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(BrightBlue, BrightPurple, BrightCyan))
            ).padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(.18f)) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("SMART DAY PLANNER", color = Color.White, fontWeight = FontWeight.Black)
                }
                Text("Turn your timetable\\ninto reminders.", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("\${count} active reminders • on-device timetable processing", color = Color.White.copy(.88f))
                Button(
                    onClick = onUpload,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrightBlue)
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload timetable screenshot", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = BrightBlue)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyState(onUpload: () -> Unit, onAdd: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.CalendarMonth, null, tint = BrightPurple, modifier = Modifier.size(52.dp))
            Text("Nothing scheduled yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Upload your college timetable and turn it into a weekly reminder schedule.")
            Button(onClick = onUpload, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("Choose screenshot")
            }
            OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(14.dp)) { Text("Create manually") }
        }
    }
}

@Composable
private fun ReminderCard(r: Reminder, vm: ReminderViewModel) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp, 58.dp).clip(RoundedCornerShape(8.dp)).background(if (r.enabled) BrightBlue else Color.LightGray))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.triggerAt)))
                if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.bodySmall)
                Text(if (r.repeat == "NONE") "One time" else r.repeat.lowercase().replaceFirstChar { it.uppercase() }, color = BrightPurple, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { vm.update(r.copy(enabled = !r.enabled)) }) { Text(if (r.enabled) "On" else "Off") }
                TextButton(onClick = { vm.delete(r) }) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun AddReminderDialog(onDismiss: () -> Unit, onSave: (String, String, Long, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("NONE") }
    val initial = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 5); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) } }
    var selected by remember { mutableStateOf(initial.timeInMillis) }
    val context = LocalContext.current
    fun pick() {
        val base = Calendar.getInstance().apply { timeInMillis = selected }
        android.app.DatePickerDialog(context, { _, y, m, d ->
            android.app.TimePickerDialog(context, { _, h, min ->
                base.set(y, m, d, h, min, 0); base.set(Calendar.MILLISECOND, 0); selected = base.timeInMillis
            }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), false).show()
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create reminder", fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("What should I remind you about?") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
            OutlinedButton(onClick = ::pick, modifier = Modifier.fillMaxWidth()) {
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(selected)))
            }
            Text("Repeat", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("NONE", "DAILY", "WEEKLY").forEach { option ->
                    FilterChip(selected = repeat == option, onClick = { repeat = option }, label = { Text(option) })
                }
            }
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title.trim(), note.trim(), selected, repeat) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
