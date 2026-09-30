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
                val c = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, entry.dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, entry.hour)
                    set(Calendar.MINUTE, entry.minute)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
                    add(Calendar.MINUTE, -leadMinutes)
                }
                dao.insert(Reminder(
                    title = "Class: ${entry.title}",
                    note = "Imported from timetable",
                    triggerAt = c.timeInMillis,
                    repeat = "WEEKLY",
                    enabled = true,
                    repeatDayOfWeek = entry.dayOfWeek
                ))
            }
            dao.getEnabled().forEach { ReminderScheduler.schedule(getApplication(), it) }
            onComplete()
        }
    }
}
