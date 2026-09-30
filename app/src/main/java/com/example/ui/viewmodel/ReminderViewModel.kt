package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartReminderApp
import com.example.alarm.ReminderScheduler
import com.example.data.model.Reminder
import com.example.data.model.RepeatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ReminderFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    REPEATING("Repeating"),
    COMPLETED("Completed")
}

class ReminderViewModel : ViewModel() {

    private val repository = SmartReminderApp.instance.repository
    private val scheduler = SmartReminderApp.instance.scheduler

    val allReminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ReminderFilter.ALL)
    val selectedFilter: StateFlow<ReminderFilter> = _selectedFilter.asStateFlow()

    private val _lastDeletedReminder = MutableStateFlow<Reminder?>(null)
    val lastDeletedReminder: StateFlow<Reminder?> = _lastDeletedReminder.asStateFlow()

    val filteredReminders: StateFlow<List<Reminder>> = combine(
        allReminders,
        _searchQuery,
        _selectedFilter
    ) { reminders, query, filter ->
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        reminders.filter { reminder ->
            val matchesQuery = query.isBlank() ||
                    reminder.title.contains(query, ignoreCase = true) ||
                    reminder.description.contains(query, ignoreCase = true)

            if (!matchesQuery) return@filter false

            when (filter) {
                ReminderFilter.ALL -> true
                ReminderFilter.TODAY -> {
                    val reminderDate = Instant.ofEpochMilli(reminder.scheduledTimeMillis)
                        .atZone(zoneId)
                        .toLocalDate()
                    reminderDate == today
                }
                ReminderFilter.UPCOMING -> {
                    !reminder.isCompleted && reminder.scheduledTimeMillis > now
                }
                ReminderFilter.REPEATING -> {
                    reminder.getRepeatTypeEnum() != RepeatType.NONE
                }
                ReminderFilter.COMPLETED -> {
                    reminder.isCompleted
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCount: StateFlow<Int> = allReminders.combine(MutableStateFlow(Unit)) { list, _ ->
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        list.count {
            val date = Instant.ofEpochMilli(it.scheduledTimeMillis).atZone(zoneId).toLocalDate()
            date == today && !it.isCompleted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val nextUpcoming: StateFlow<Reminder?> = allReminders.combine(MutableStateFlow(Unit)) { list, _ ->
        val now = System.currentTimeMillis()
        list.filter { !it.isCompleted && it.isEnabled && it.scheduledTimeMillis > now }
            .minByOrNull { it.scheduledTimeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ReminderFilter) {
        _selectedFilter.value = filter
    }

    fun saveReminder(reminder: Reminder) {
        viewModelScope.launch {
            if (reminder.id == 0L) {
                val newId = repository.insertReminder(reminder)
                val inserted = reminder.copy(id = newId)
                scheduler.scheduleReminder(inserted)
            } else {
                repository.updateReminder(reminder)
                scheduler.cancelReminder(reminder)
                scheduler.scheduleReminder(reminder)
            }
        }
    }

    fun toggleCompleted(reminder: Reminder) {
        viewModelScope.launch {
            val newCompleted = !reminder.isCompleted
            val updated = reminder.copy(
                isCompleted = newCompleted,
                updatedTimestamp = System.currentTimeMillis()
            )
            repository.updateReminder(updated)

            if (newCompleted) {
                scheduler.cancelReminder(reminder)
                // If it repeats, calculate the next occurrence and schedule it as uncompleted
                if (reminder.getRepeatTypeEnum() != RepeatType.NONE) {
                    val nextTime = ReminderScheduler.calculateNextOccurrence(
                        reminder,
                        System.currentTimeMillis()
                    )
                    val nextOccurrenceReminder = reminder.copy(
                        scheduledTimeMillis = nextTime,
                        isCompleted = false,
                        updatedTimestamp = System.currentTimeMillis()
                    )
                    repository.updateReminder(nextOccurrenceReminder)
                    scheduler.scheduleReminder(nextOccurrenceReminder)
                }
            } else {
                // Unmarked completed: re-schedule if future
                scheduler.scheduleReminder(updated)
            }
        }
    }

    fun toggleEnabled(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(
                isEnabled = !reminder.isEnabled,
                updatedTimestamp = System.currentTimeMillis()
            )
            repository.updateReminder(updated)
            if (updated.isEnabled) {
                scheduler.scheduleReminder(updated)
            } else {
                scheduler.cancelReminder(reminder)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            scheduler.cancelReminder(reminder)
            repository.deleteReminder(reminder)
            _lastDeletedReminder.value = reminder
        }
    }

    fun restoreLastDeleted() {
        val deleted = _lastDeletedReminder.value ?: return
        viewModelScope.launch {
            val restored = deleted.copy(id = 0)
            val newId = repository.insertReminder(restored)
            val finalReminder = restored.copy(id = newId)
            scheduler.scheduleReminder(finalReminder)
            _lastDeletedReminder.value = null
        }
    }

    fun clearLastDeleted() {
        _lastDeletedReminder.value = null
    }

    fun snoozeReminder(reminder: Reminder, minutes: Int) {
        scheduler.scheduleSnooze(
            reminderId = reminder.id,
            notificationId = reminder.notificationId,
            title = reminder.title,
            description = reminder.description,
            snoozeMinutes = minutes
        )
    }
}
