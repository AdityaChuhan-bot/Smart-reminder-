package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY scheduledTimeMillis ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderById(id: Long): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderByIdSync(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 AND (scheduledTimeMillis - advanceMinutes * 60000) > :currentTimeMillis ORDER BY scheduledTimeMillis ASC")
    suspend fun getActiveFutureRemindersSync(currentTimeMillis: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 ORDER BY scheduledTimeMillis ASC")
    suspend fun getAllActiveRemindersSync(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReminders(reminders: List<Reminder>)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
