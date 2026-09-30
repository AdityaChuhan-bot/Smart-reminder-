package com.adityachuhan.smartreminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adityachuhan.smartreminder.data.AppDatabase
import com.adityachuhan.smartreminder.data.Reminder
import kotlinx.coroutines.launch
import java.util.Calendar

class TimetableViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).reminderDao()

    fun importEntries(entries: List<TimetableEntry>, leadMinutes: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            entries.forEach { entry ->
                val classTime = nextClassTime(entry.dayOfWeek, entry.hour, entry.minute)
                classTime.add(Calendar.MINUTE, -leadMinutes)
                dao.insert(
                    Reminder(
                        title = "Class: ${entry.title}",
                        note = "Imported from timetable",
                        triggerAt = classTime.timeInMillis,
                        repeat = "WEEKLY",
                        enabled = true,
                        repeatDayOfWeek = entry.dayOfWeek
                    )
                )
            }
            dao.getEnabled().forEach { ReminderScheduler.schedule(getApplication(), it) }
            onComplete()
        }
    }

    private fun nextClassTime(day: Int, hour: Int, minute: Int): Calendar {
        val now = Calendar.getInstance()
        val c = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now.timeInMillis) c.add(Calendar.WEEK_OF_YEAR, 1)
        return c
    }
}
