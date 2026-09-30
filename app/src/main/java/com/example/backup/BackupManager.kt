package com.example.backup

import com.example.data.model.Reminder
import com.example.data.model.TimetableClass
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val reminders: List<Reminder>,
    val timetableClasses: List<TimetableClass>
)

object BackupManager {

    private const val CURRENT_VERSION = 1

    /**
     * Serializes reminders and timetable classes to a formatted JSON string.
     */
    fun createBackupJson(
        reminders: List<Reminder>,
        classes: List<TimetableClass>
    ): String {
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "Smart Reminder")

        // Reminders array
        val remindersArray = JSONArray()
        for (r in reminders) {
            val obj = JSONObject()
            obj.put("title", r.title)
            obj.put("description", r.description)
            obj.put("scheduledTimeMillis", r.scheduledTimeMillis)
            obj.put("repeatType", r.repeatType)
            obj.put("repeatDays", r.repeatDays)
            obj.put("isEnabled", r.isEnabled)
            obj.put("isCompleted", r.isCompleted)
            obj.put("advanceMinutes", r.advanceMinutes)
            obj.put("createdTimestamp", r.createdTimestamp)
            remindersArray.put(obj)
        }
        root.put("reminders", remindersArray)

        // Classes array
        val classesArray = JSONArray()
        for (c in classes) {
            val obj = JSONObject()
            obj.put("subject", c.subject)
            obj.put("teacher", c.teacher)
            obj.put("room", c.room)
            obj.put("startHour", c.startHour)
            obj.put("startMinute", c.startMinute)
            obj.put("endHour", c.endHour)
            obj.put("endMinute", c.endMinute)
            obj.put("daysOfWeek", c.daysOfWeek)
            obj.put("advanceMinutes", c.advanceMinutes)
            obj.put("isEnabled", c.isEnabled)
            classesArray.put(obj)
        }
        root.put("timetableClasses", classesArray)

        return root.toString(2)
    }

    /**
     * Parses and validates a backup JSON string.
     * Generates fresh IDs to avoid primary key collisions.
     */
    fun parseBackupJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val remindersList = mutableListOf<Reminder>()
        val classesList = mutableListOf<TimetableClass>()

        val remindersArray = root.optJSONArray("reminders")
        if (remindersArray != null) {
            for (i in 0 until remindersArray.length()) {
                val obj = remindersArray.getJSONObject(i)
                val title = obj.optString("title", "Reminder").trim()
                if (title.isBlank()) continue

                val reminder = Reminder(
                    id = 0, // autoGenerate new ID
                    title = title,
                    description = obj.optString("description", ""),
                    scheduledTimeMillis = obj.optLong("scheduledTimeMillis", System.currentTimeMillis()),
                    repeatType = obj.optString("repeatType", "NONE"),
                    repeatDays = obj.optString("repeatDays", ""),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    advanceMinutes = obj.optInt("advanceMinutes", 0),
                    createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
                    updatedTimestamp = System.currentTimeMillis()
                )
                remindersList.add(reminder)
            }
        }

        val classesArray = root.optJSONArray("timetableClasses")
        if (classesArray != null) {
            for (i in 0 until classesArray.length()) {
                val obj = classesArray.getJSONObject(i)
                val subject = obj.optString("subject", "").trim()
                if (subject.isBlank()) continue

                val cls = TimetableClass(
                    id = 0, // autoGenerate new ID
                    subject = subject,
                    teacher = obj.optString("teacher", ""),
                    room = obj.optString("room", ""),
                    startHour = obj.optInt("startHour", 9),
                    startMinute = obj.optInt("startMinute", 0),
                    endHour = obj.optInt("endHour", 10),
                    endMinute = obj.optInt("endMinute", 0),
                    daysOfWeek = obj.optString("daysOfWeek", "1,2,3,4,5"),
                    advanceMinutes = obj.optInt("advanceMinutes", 5),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                classesList.add(cls)
            }
        }

        return BackupPayload(reminders = remindersList, timetableClasses = classesList)
    }
}
