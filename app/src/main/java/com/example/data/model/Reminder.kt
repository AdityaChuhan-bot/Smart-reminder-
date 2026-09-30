package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduledTimeMillis: Long,
    val repeatType: String = RepeatType.NONE.name,
    // Comma-separated day numbers (1=Monday, 7=Sunday) for CUSTOM repeat
    val repeatDays: String = "",
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val notificationId: Int = (System.currentTimeMillis() % 1000000).toInt(),
    val advanceMinutes: Int = 0, // 0 = at time, 5, 10, 15, 30, 60, 1440
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    fun getRepeatTypeEnum(): RepeatType = RepeatType.fromString(repeatType)

    fun getTriggerTimeMillis(): Long {
        return scheduledTimeMillis - (advanceMinutes * 60 * 1000L)
    }
}
