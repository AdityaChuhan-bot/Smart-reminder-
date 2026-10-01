package com.adityachuhan.smartreminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adityachuhan.smartreminder.data.AppDatabase
import com.adityachuhan.smartreminder.data.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).reminderDao()
    val reminders = dao.observeAll().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun add(title: String, note: String, triggerAt: Long, repeat: String, repeatDayOfWeek: Int? = null) {
        viewModelScope.launch {
            val id = dao.insert(
                Reminder(
                    title = title,
                    note = note,
                    triggerAt = triggerAt,
                    repeat = repeat,
                    repeatDayOfWeek = repeatDayOfWeek,
                    enabled = true
                )
            )
            dao.getById(id)?.let {
                ReminderScheduler.schedule(getApplication(), it)
            }
        }
    }

    fun update(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), reminder)
            dao.update(reminder)
            if (reminder.enabled) {
                ReminderScheduler.schedule(getApplication(), reminder)
            }
        }
    }

    fun snooze(reminder: Reminder, minutes: Int = 10) {
        viewModelScope.launch {
            val newTime = System.currentTimeMillis() + minutes * 60 * 1000L
            val snoozed = reminder.copy(triggerAt = newTime, enabled = true)
            update(snoozed)
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), reminder)
            dao.delete(reminder)
        }
    }
}
