package com.adityachuhan.smartreminder

import android.Manifest
import android.app.*
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityachuhan.smartreminder.data.Reminder
import java.text.DateFormat
import java.util.*

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
        setContent { MaterialTheme { ReminderApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderApp(vm: ReminderViewModel = viewModel()) {
    val reminders by vm.reminders.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart Reminder") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }
    ) { padding ->
        if (reminders.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No reminders", style = MaterialTheme.typography.headlineSmall)
                Text("Tap + to create your first reminder.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                items(reminders, key = { it.id }) { r -> ReminderCard(r, vm) }
            }
        }
    }
    if (showAdd) AddReminderDialog(
        onDismiss = { showAdd = false },
        onSave = { title, note, time, repeat -> vm.add(title, note, time, repeat); showAdd = false }
    )
}

@Composable
private fun ReminderCard(r: Reminder, vm: ReminderViewModel) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleLarge)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.triggerAt)))
            if (r.note.isNotBlank()) Text(r.note)
            Text(if (r.repeat == "NONE") "One time" else r.repeat.lowercase().replaceFirstChar { it.uppercase() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.update(r.copy(enabled = !r.enabled)) }) { Text(if (r.enabled) "Disable" else "Enable") }
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

    fun pickDateTime() {
        val base = Calendar.getInstance().apply { timeInMillis = selected }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                base.set(y, m, d, h, min, 0)
                base.set(Calendar.MILLISECOND, 0)
                selected = base.timeInMillis
            }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), false).show()
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
                TextButton(onClick = { pickDateTime() }) {
                    Text("When: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(selected)))
                }
                Text("Repeat")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NONE", "DAILY", "WEEKLY").forEach { option ->
                        FilterChip(selected = repeat == option, onClick = { repeat = option }, label = { Text(option) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(title.trim(), note.trim(), selected, repeat) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
