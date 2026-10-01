package com.adityachuhan.smartreminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adityachuhan.smartreminder.data.AppDatabase
import com.adityachuhan.smartreminder.data.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).todoDao()

    val todos = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun add(
        title: String,
        note: String = "",
        priority: String = "NORMAL",
        dueDate: Long? = null,
        category: String = "General"
    ) {
        viewModelScope.launch {
            dao.insert(
                TodoItem(
                    title = title.trim(),
                    note = note.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    category = category
                )
            )
        }
    }

    fun toggleCompleted(todo: TodoItem) {
        viewModelScope.launch {
            dao.update(todo.copy(isCompleted = !todo.isCompleted))
        }
    }

    fun update(todo: TodoItem) {
        viewModelScope.launch {
            dao.update(todo)
        }
    }

    fun delete(todo: TodoItem) {
        viewModelScope.launch {
            dao.delete(todo)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            dao.clearCompleted()
        }
    }
}
