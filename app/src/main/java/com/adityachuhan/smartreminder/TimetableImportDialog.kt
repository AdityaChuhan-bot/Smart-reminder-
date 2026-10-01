package com.adityachuhan.smartreminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TimetableImportDialog(entries: List<TimetableEntry>, onDismiss: () -> Unit, onImport: (Int) -> Unit) {
    var lead by remember { mutableStateOf("10") }
    val days = listOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text("Timetable found", fontWeight = FontWeight.Black)
                Text("${entries.size} classes detected", style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEFF6FF)) {
                    Text("Review the detected classes before they become weekly reminders.", Modifier.padding(14.dp))
                }
                OutlinedTextField(
                    value = lead,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) lead = it },
                    label = { Text("Remind me before class (minutes)") },
                    singleLine = true
                )
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(entries) { e ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF7F7FF)) {
                            Text("${days.getOrElse(e.dayOfWeek) { "" }}  %02d:%02d  •  ${e.title}".format(e.hour, e.minute), Modifier.padding(11.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onImport(lead.toIntOrNull() ?: 10) }, enabled = entries.isNotEmpty()) {
                Text("Create weekly reminders", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
