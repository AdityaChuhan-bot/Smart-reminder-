package com.adityachuhan.smartreminder.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adityachuhan.smartreminder.data.TodoItem
import com.adityachuhan.smartreminder.ui.theme.DangerRed
import com.adityachuhan.smartreminder.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTodoDialog(
    initialTodo: TodoItem? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, priority: String, dueDate: Long?, category: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTodo?.title ?: "") }
    var note by remember { mutableStateOf(initialTodo?.note ?: "") }
    var priority by remember { mutableStateOf(initialTodo?.priority ?: "NORMAL") }
    var category by remember { mutableStateOf(initialTodo?.category ?: "General") }
    var dueDate by remember { mutableStateOf<Long?>(initialTodo?.dueDate) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    fun showDatePicker() {
        val cal = Calendar.getInstance().apply {
            dueDate?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 0)
                }
                dueDate = chosen.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val categories = listOf("General", "Study", "Assignment", "Personal", "Work", "Errands")
    val priorities = listOf("LOW" to "Low", "NORMAL" to "Normal", "HIGH" to "High")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = if (initialTodo == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Track your action items and assignments",
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
                    label = { Text("Task title *") },
                    placeholder = { Text("e.g. Read Chapter 4, Submit Paper") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Details / Note (optional)") },
                    placeholder = { Text("Steps, links, checklist details...") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selection
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    priorities.forEach { (key, label) ->
                        val isSelected = priority == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = key },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected && key == "HIGH") DangerRed else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Due Date (Optional)
                Text(
                    text = "Due Date (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = ::showDatePicker,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = dueDate?.let { dateFormatter.format(Date(it)) } ?: "Set Due Date",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (dueDate != null) {
                        IconButton(onClick = { dueDate = null }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove due date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        onSave(title.trim(), note.trim(), priority, dueDate, category)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text(
                    text = if (initialTodo == null) "Add Task" else "Update Task",
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
