package com.adityachuhan.smartreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "NORMAL", // "HIGH", "NORMAL", "LOW"
    val dueDate: Long? = null,
    val category: String = "General", // "Study", "Assignment", "Personal", "Work", "Errands"
    val createdTimestamp: Long = System.currentTimeMillis()
)
