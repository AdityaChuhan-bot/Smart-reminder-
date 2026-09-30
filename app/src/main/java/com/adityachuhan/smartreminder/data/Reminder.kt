package com.adityachuhan.smartreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val triggerAt: Long,
    val repeat: String = "NONE",
    val enabled: Boolean = true
)
