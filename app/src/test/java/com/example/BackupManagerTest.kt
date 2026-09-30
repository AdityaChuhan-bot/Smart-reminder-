package com.example

import com.example.backup.BackupManager
import com.example.data.model.Reminder
import com.example.data.model.TimetableClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    @Test
    fun testExportAndImportCycle() {
        val reminders = listOf(
            Reminder(
                id = 101,
                title = "Study for Finals",
                description = "Chapters 1 to 5",
                scheduledTimeMillis = 1770000000000L,
                repeatType = "DAILY",
                advanceMinutes = 10
            )
        )

        val classes = listOf(
            TimetableClass(
                id = 202,
                subject = "Linear Algebra",
                teacher = "Dr. Gauss",
                room = "Hall 1",
                startHour = 10,
                startMinute = 15,
                endHour = 11,
                endMinute = 45,
                daysOfWeek = "1,3,5"
            )
        )

        val jsonString = BackupManager.createBackupJson(reminders, classes)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("Study for Finals"))
        assertTrue(jsonString.contains("Linear Algebra"))

        val parsed = BackupManager.parseBackupJson(jsonString)
        assertEquals(1, parsed.reminders.size)
        assertEquals(1, parsed.timetableClasses.size)

        val parsedReminder = parsed.reminders[0]
        assertEquals("Study for Finals", parsedReminder.title)
        assertEquals("Chapters 1 to 5", parsedReminder.description)
        assertEquals(10, parsedReminder.advanceMinutes)
        // Verify fresh ID 0 is assigned for safe insertion
        assertEquals(0L, parsedReminder.id)

        val parsedClass = parsed.timetableClasses[0]
        assertEquals("Linear Algebra", parsedClass.subject)
        assertEquals("Dr. Gauss", parsedClass.teacher)
        assertEquals("Hall 1", parsedClass.room)
        assertEquals(0L, parsedClass.id)
    }
}
