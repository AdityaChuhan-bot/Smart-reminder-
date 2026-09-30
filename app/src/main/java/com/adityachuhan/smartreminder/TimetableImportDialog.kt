package com.adityachuhan.smartreminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimetableImportDialog(entries: List<TimetableEntry>, onDismiss: () -> Unit, onImport: (Int) -> Unit) {
    var lead by remember { mutableStateOf("10") }
    val days = listOf("","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review timetable") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Detected ${entries.size} entries. Review the OCR result before creating reminders.")
                OutlinedTextField(
                    value = lead,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) lead = it },
                    label = { Text("Remind me before class (minutes)") },
                    singleLine = true
                )
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(entries) { e ->
                        Text("• ${days[e.dayOfWeek]} %02d:%02d — ${e.title}".format(e.hour, e.minute))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(lead.toIntOrNull() ?: 10) }, enabled = entries.isNotEmpty()) {
                Text("Import reminders")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
