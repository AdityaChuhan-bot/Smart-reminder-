package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_classes")
data class TimetableClass(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val teacher: String = "",
    val room: String = "",
    val startHour: Int,    // 0 - 23
    val startMinute: Int,  // 0 - 59
    val endHour: Int,      // 0 - 23
    val endMinute: Int,    // 0 - 59
    // Comma-separated day numbers (1=Monday, 7=Sunday)
    val daysOfWeek: String = "1,2,3,4,5",
    val advanceMinutes: Int = 5, // 0, 5, 10, 15, 30
    val isEnabled: Boolean = true,
    val notificationId: Int = (System.currentTimeMillis() % 1000000).toInt() + 10000,
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTimeRange(): String {
        val startFormatted = String.format("%02d:%02d", startHour, startMinute)
        val endFormatted = String.format("%02d:%02d", endHour, endMinute)
        return "$startFormatted - $endFormatted"
    }

    fun getDaysList(): List<Int> {
        if (daysOfWeek.isBlank()) return emptyList()
        return daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
