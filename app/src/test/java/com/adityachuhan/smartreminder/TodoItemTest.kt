package com.adityachuhan.smartreminder

import com.adityachuhan.smartreminder.data.TodoItem
import org.junit.Assert.*
import org.junit.Test

class TodoItemTest {

    @Test
    fun testTodoItemCreationDefaults() {
        val todo = TodoItem(
            title = "Complete Math Assignment"
        )
        assertEquals("Complete Math Assignment", todo.title)
        assertEquals("", todo.note)
        assertFalse(todo.isCompleted)
        assertEquals("NORMAL", todo.priority)
        assertNull(todo.dueDate)
        assertEquals("General", todo.category)
        assertTrue(todo.createdTimestamp > 0)
    }

    @Test
    fun testTodoItemCompletionToggle() {
        val initial = TodoItem(
            title = "Study Physics",
            isCompleted = false
        )
        val completed = initial.copy(isCompleted = !initial.isCompleted)
        assertTrue(completed.isCompleted)

        val uncompleted = completed.copy(isCompleted = !completed.isCompleted)
        assertFalse(uncompleted.isCompleted)
    }

    @Test
    fun testTodoItemCustomFields() {
        val dueDate = System.currentTimeMillis() + 86400000L
        val todo = TodoItem(
            id = 10,
            title = "Lab Report",
            note = "Submit PDF to portal",
            isCompleted = false,
            priority = "HIGH",
            dueDate = dueDate,
            category = "Study"
        )
        assertEquals(10L, todo.id)
        assertEquals("HIGH", todo.priority)
        assertEquals("Study", todo.category)
        assertEquals(dueDate, todo.dueDate)
    }
}
