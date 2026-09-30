package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Reminder
import com.example.data.model.TimetableClass
import com.example.ui.common.DateTimeUtils
import com.example.ui.common.StatusBadge
import com.example.ui.home.ReminderCard
import com.example.ui.timetable.ClassCard
import com.example.ui.viewmodel.ReminderViewModel
import com.example.ui.viewmodel.TimetableViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    reminderViewModel: ReminderViewModel,
    timetableViewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val allReminders by reminderViewModel.allReminders.collectAsStateWithLifecycle()
    val allClasses by timetableViewModel.allClasses.collectAsStateWithLifecycle()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val zoneId = ZoneId.systemDefault()

    // Filter reminders for selected date
    val remindersForDate = remember(allReminders, selectedDate) {
        allReminders.filter { reminder ->
            val reminderDate = Instant.ofEpochMilli(reminder.scheduledTimeMillis)
                .atZone(zoneId)
                .toLocalDate()
            reminderDate == selectedDate
        }
    }

    // Filter classes for day of week of selected date (1=Mon, 7=Sun)
    val dayOfWeekVal = selectedDate.dayOfWeek.value
    val classesForDate = remember(allClasses, dayOfWeekVal) {
        allClasses.filter { cls ->
            cls.getDaysList().contains(dayOfWeekVal)
        }.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Month navigation header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    IconButton(
                        onClick = {
                            selectedDate = LocalDate.now()
                            currentMonth = YearMonth.now()
                        }
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Today")
                    }
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }

            // Month Grid
            CalendarMonthView(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                reminders = allReminders,
                classes = allClasses,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Day Header
            Text(
                text = "${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))} Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Combined list of reminders & classes for the day
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (remindersForDate.isEmpty() && classesForDate.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No events scheduled for this day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (classesForDate.isNotEmpty()) {
                    item {
                        Text(
                            text = "Classes (${classesForDate.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(items = classesForDate, key = { "class_${it.id}" }) { cls ->
                        ClassCard(
                            timetableClass = cls,
                            onToggleEnabled = { timetableViewModel.toggleClassEnabled(cls) },
                            onEdit = {},
                            onDelete = { timetableViewModel.deleteClass(cls) }
                        )
                    }
                }

                if (remindersForDate.isNotEmpty()) {
                    item {
                        Text(
                            text = "Reminders (${remindersForDate.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(items = remindersForDate, key = { "reminder_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleComplete = { reminderViewModel.toggleCompleted(reminder) },
                            onToggleEnabled = { reminderViewModel.toggleEnabled(reminder) },
                            onEdit = {},
                            onDelete = { reminderViewModel.deleteReminder(reminder) },
                            onSnooze = { reminderViewModel.snoozeReminder(reminder, 10) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarMonthView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    reminders: List<Reminder>,
    classes: List<TimetableClass>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1 // 0 for Mon .. 6 for Sun

    val zoneId = ZoneId.systemDefault()

    // Compute dates with reminders
    val datesWithReminders = remember(reminders, currentMonth) {
        reminders.map {
            Instant.ofEpochMilli(it.scheduledTimeMillis).atZone(zoneId).toLocalDate()
        }.toSet()
    }

    val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Days Grid (6 rows max)
        val totalCells = 42
        val cells = (0 until totalCells).map { index ->
            val dayNumber = index - dayOfWeekOffset + 1
            if (dayNumber in 1..daysInMonth) {
                currentMonth.atDay(dayNumber)
            } else {
                null
            }
        }

        val rows = cells.chunked(7)
        rows.forEach { rowCells ->
            // Only render row if it contains at least one day in the current month
            if (rowCells.any { it != null }) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowCells.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()
                                val hasReminder = datesWithReminders.contains(date)
                                val hasClasses = classes.any { it.getDaysList().contains(date.dayOfWeek.value) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else Color.Transparent
                                        )
                                        .clickable { onDateSelected(date) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )

                                    // Indicator dot row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.height(6.dp)
                                    ) {
                                        if (hasReminder) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.primary
                                                    )
                                            )
                                        }
                                        if (hasClasses) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                        else MaterialTheme.colorScheme.secondary
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
