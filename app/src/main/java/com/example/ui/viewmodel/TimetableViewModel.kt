package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartReminderApp
import com.example.data.model.TimetableClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TimetableViewModel : ViewModel() {

    private val repository = SmartReminderApp.instance.repository
    private val scheduler = SmartReminderApp.instance.scheduler

    val allClasses: StateFlow<List<TimetableClass>> = repository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1 = Monday .. 7 = Sunday
    private val _selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    val classesForSelectedDay: StateFlow<List<TimetableClass>> = combine(
        allClasses,
        _selectedDay
    ) { classes, day ->
        classes.filter { cls ->
            cls.getDaysList().contains(day)
        }.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun saveClass(timetableClass: TimetableClass) {
        viewModelScope.launch {
            if (timetableClass.id == 0L) {
                val newId = repository.insertClass(timetableClass)
                val inserted = timetableClass.copy(id = newId)
                scheduler.scheduleClass(inserted)
            } else {
                repository.updateClass(timetableClass)
                scheduler.cancelClass(timetableClass)
                scheduler.scheduleClass(timetableClass)
            }
        }
    }

    fun toggleClassEnabled(timetableClass: TimetableClass) {
        viewModelScope.launch {
            val updated = timetableClass.copy(isEnabled = !timetableClass.isEnabled)
            repository.updateClass(updated)
            if (updated.isEnabled) {
                scheduler.scheduleClass(updated)
            } else {
                scheduler.cancelClass(timetableClass)
            }
        }
    }

    fun deleteClass(timetableClass: TimetableClass) {
        viewModelScope.launch {
            scheduler.cancelClass(timetableClass)
            repository.deleteClass(timetableClass)
        }
    }

    fun importClasses(classes: List<TimetableClass>) {
        viewModelScope.launch {
            for (cls in classes) {
                val id = repository.insertClass(cls)
                val inserted = cls.copy(id = id)
                scheduler.scheduleClass(inserted)
            }
        }
    }
}
