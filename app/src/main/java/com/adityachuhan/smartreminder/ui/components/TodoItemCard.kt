package com.adityachuhan.smartreminder.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adityachuhan.smartreminder.data.TodoItem
import com.adityachuhan.smartreminder.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TodoItemCard(
    todo: TodoItem,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onScheduleReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = todo.isCompleted
    val now = System.currentTimeMillis()

    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val isOverdue = todo.dueDate != null && todo.dueDate < now && !isCompleted

    val dueText = todo.dueDate?.let { due ->
        val calDue = Calendar.getInstance().apply { timeInMillis = due }
        val calNow = Calendar.getInstance()
        val isToday = calDue.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
        val isTomorrow = calDue.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) + 1

        when {
            isOverdue -> "Overdue • ${dateFormat.format(Date(due))}"
            isToday -> "Due Today"
            isTomorrow -> "Due Tomorrow"
            else -> "Due ${dateFormat.format(Date(due))}"
        }
    }

    val (priorityLabel, priorityColor, priorityBg) = when (todo.priority) {
        "HIGH" -> Triple("High", DangerRed, DangerRedContainer)
        "LOW" -> Triple("Low", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        else -> Triple("Normal", IndigoPrimary, IndigoContainer)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isOverdue) {
                    Modifier.border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Checkbox + Title + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleCompleted() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = IndigoPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Priority Badge
                if (todo.priority == "HIGH" || !isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityBg,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = priorityColor
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = priorityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            )
                        }
                    }
                }
            }

            // Note if present
            if (todo.note.isNotBlank()) {
                Text(
                    text = todo.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 44.dp)
                )
            }

            // Bottom Badges & Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category & Due date badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PurpleContainer
                    ) {
                        Text(
                            text = todo.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PurpleOnContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Due Date Badge
                    if (dueText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOverdue) DangerRedContainer else CyanContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = if (isOverdue) DangerRed else CyanOnContainer
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = dueText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdue) DangerRed else CyanOnContainer
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Convert / Schedule Alarm for this task
                    IconButton(
                        onClick = onScheduleReminder,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmAdd,
                            contentDescription = "Set alarm reminder for task",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = DangerRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
