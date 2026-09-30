package com.example.timetable

import com.example.data.model.TimetableClass
import java.util.Locale
import java.util.regex.Pattern

data class ParsedClassPreview(
    val dayNumber: Int, // 1 = Monday .. 7 = Sunday
    val dayName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val subject: String,
    val room: String = "",
    val teacher: String = "",
    var isSelected: Boolean = true
) {
    fun toTimetableClass(): TimetableClass {
        return TimetableClass(
            subject = subject,
            teacher = teacher,
            room = room,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            daysOfWeek = dayNumber.toString(),
            advanceMinutes = 5,
            isEnabled = true
        )
    }

    fun getFormattedTime(): String {
        return String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }
}

object TimetableParser {

    private val DAY_MAP = mapOf(
        "monday" to 1, "mon" to 1,
        "tuesday" to 2, "tue" to 2, "tues" to 2,
        "wednesday" to 3, "wed" to 3,
        "thursday" to 4, "thu" to 4, "thur" to 4, "thurs" to 4,
        "friday" to 5, "fri" to 5,
        "saturday" to 6, "sat" to 6,
        "sunday" to 7, "sun" to 7
    )

    private val DAY_NAMES = arrayOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    // Regex for: "09:30 - 10:30" or "9:30-10:30" or "9:30"
    // Supports 12h with am/pm or 24h
    private val TIME_RANGE_PATTERN = Pattern.compile(
        """^(\d{1,2})[:.](\d{2})\s*(am|pm)?\s*(?:[-–to]+\s*(\d{1,2})[:.](\d{2})\s*(am|pm)?)?\s*[-–:]?\s*(.+)$""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(text: String): List<ParsedClassPreview> {
        val results = mutableListOf<ParsedClassPreview>()
        val lines = text.lines()

        var currentDay = 1 // Default Monday

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) continue

            val lowerLine = line.lowercase(Locale.getDefault())

            // Check if line is a day header
            val matchedDay = DAY_MAP[lowerLine.removeSuffix(":").trim()]
            if (matchedDay != null) {
                currentDay = matchedDay
                continue
            }

            // Check if line matches time pattern
            val matcher = TIME_RANGE_PATTERN.matcher(line)
            if (matcher.find()) {
                var sHour = matcher.group(1)?.toIntOrNull() ?: continue
                val sMin = matcher.group(2)?.toIntOrNull() ?: 0
                val sAmPm = matcher.group(3)?.lowercase(Locale.getDefault())

                if (sAmPm == "pm" && sHour < 12) sHour += 12
                if (sAmPm == "am" && sHour == 12) sHour = 0

                val rawEndHour = matcher.group(4)?.toIntOrNull()
                val rawEndMin = matcher.group(5)?.toIntOrNull() ?: 0
                val eAmPm = matcher.group(6)?.lowercase(Locale.getDefault())

                var eHour: Int
                var eMin: Int
                if (rawEndHour != null) {
                    eHour = rawEndHour
                    eMin = rawEndMin
                    if (eAmPm == "pm" && eHour < 12) eHour += 12
                    if (eAmPm == "am" && eHour == 12) eHour = 0
                } else {
                    // Default 1 hour duration
                    eHour = (sHour + 1) % 24
                    eMin = sMin
                }

                val remainingText = matcher.group(7)?.trim() ?: continue

                // Extract room & teacher heuristics
                var subject = remainingText
                var room = ""
                var teacher = ""

                // Check for Room patterns, e.g. "Room 301" or "Rm 12" or "Lab 3"
                val roomRegex = """(?i)\b(?:room|rm|lab|hall)\s*([A-Za-z0-9\-]+)""".toRegex()
                val roomMatch = roomRegex.find(remainingText)
                if (roomMatch != null) {
                    room = roomMatch.value
                    subject = subject.replace(roomMatch.value, "").trim()
                }

                // Check for Teacher patterns, e.g. "with Mr. John" or "Dr. Smith"
                val teacherRegex = """(?i)(?:with\s+)?\b(?:mr\.|mrs\.|ms\.|dr\.|prof\.)\s*[A-Za-z]+""".toRegex()
                val teacherMatch = teacherRegex.find(subject)
                if (teacherMatch != null) {
                    teacher = teacherMatch.value.removePrefix("with ").trim()
                    subject = subject.replace(teacherMatch.value, "").trim()
                }

                // Clean remaining punctuation in subject
                subject = subject.trim('-', ',', '|', '(', ')', ' ', ':')
                if (subject.isBlank()) {
                    subject = "Class"
                }

                results.add(
                    ParsedClassPreview(
                        dayNumber = currentDay,
                        dayName = DAY_NAMES.getOrElse(currentDay - 1) { "Day $currentDay" },
                        startHour = sHour.coerceIn(0, 23),
                        startMinute = sMin.coerceIn(0, 59),
                        endHour = eHour.coerceIn(0, 23),
                        endMinute = eMin.coerceIn(0, 59),
                        subject = subject,
                        room = room,
                        teacher = teacher,
                        isSelected = true
                    )
                )
            }
        }

        return results
    }
}
