package com.example.data.repository

import com.example.data.database.ReminderDao
import com.example.data.database.TimetableDao
import com.example.data.model.Reminder
import com.example.data.model.TimetableClass
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val timetableDao: TimetableDao
) {
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val allClasses: Flow<List<TimetableClass>> = timetableDao.getAllClasses()

    fun getReminderById(id: Long): Flow<Reminder?> = reminderDao.getReminderById(id)
    suspend fun getReminderByIdSync(id: Long): Reminder? = reminderDao.getReminderByIdSync(id)

    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    suspend fun insertAllReminders(reminders: List<Reminder>) = reminderDao.insertAllReminders(reminders)
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
    suspend fun deleteReminderById(id: Long) = reminderDao.deleteReminderById(id)

    fun getClassById(id: Long): Flow<TimetableClass?> = timetableDao.getClassById(id)
    suspend fun getClassByIdSync(id: Long): TimetableClass? = timetableDao.getClassByIdSync(id)
    suspend fun insertClass(timetableClass: TimetableClass): Long = timetableDao.insertClass(timetableClass)
    suspend fun insertAllClasses(classes: List<TimetableClass>) = timetableDao.insertAllClasses(classes)
    suspend fun updateClass(timetableClass: TimetableClass) = timetableDao.updateClass(timetableClass)
    suspend fun deleteClass(timetableClass: TimetableClass) = timetableDao.deleteClass(timetableClass)
    suspend fun deleteClassById(id: Long) = timetableDao.deleteClassById(id)

    suspend fun getActiveRemindersSync(): List<Reminder> = reminderDao.getAllActiveRemindersSync()
    suspend fun getActiveClassesSync(): List<TimetableClass> = timetableDao.getActiveClassesSync()

    suspend fun clearAllData() {
        reminderDao.deleteAllReminders()
        timetableDao.deleteAllClasses()
    }
}
