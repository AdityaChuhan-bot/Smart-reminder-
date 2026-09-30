package com.example

import com.example.alarm.ReminderScheduler
import com.example.data.model.Reminder
import com.example.data.model.RepeatType
import com.example.data.model.TimetableClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderSchedulerTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")

    @Test
    fun testDailyRepeatCalculation() {
        // Scheduled: Oct 1, 2026 at 09:00 UTC
        val scheduledDateTime = LocalDateTime.of(2026, 10, 1, 9, 0)
        val scheduledMillis = scheduledDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val reminder = Reminder(
            id = 1,
            title = "Daily Standup",
            scheduledTimeMillis = scheduledMillis,
            repeatType = RepeatType.DAILY.name
        )

        // Evaluate from 10:00 UTC on the same day (Oct 1)
        val fromDateTime = LocalDateTime.of(2026, 10, 1, 10, 0)
        val fromMillis = fromDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val nextTrigger = ReminderScheduler.calculateNextOccurrence(reminder, fromMillis, zoneId)
        val nextDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTrigger), zoneId)

        // Expected: Oct 2, 2026 at 09:00 UTC
        assertEquals(2026, nextDateTime.year)
        assertEquals(10, nextDateTime.monthValue)
        assertEquals(2, nextDateTime.dayOfMonth)
        assertEquals(9, nextDateTime.hour)
        assertEquals(0, nextDateTime.minute)
    }

    @Test
    fun testWeekdayRepeatSkipsWeekend() {
        // Oct 2, 2026 is Friday
        val friday = LocalDateTime.of(2026, 10, 2, 9, 0)
        val scheduledMillis = friday.atZone(zoneId).toInstant().toEpochMilli()

        val reminder = Reminder(
            id = 2,
            title = "Weekday Class",
            scheduledTimeMillis = scheduledMillis,
            repeatType = RepeatType.WEEKDAYS.name
        )

        // Evaluate from Friday afternoon (14:00 UTC)
        val fromMillis = LocalDateTime.of(2026, 10, 2, 14, 0).atZone(zoneId).toInstant().toEpochMilli()

        val nextTrigger = ReminderScheduler.calculateNextOccurrence(reminder, fromMillis, zoneId)
        val nextDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTrigger), zoneId)

        // Next weekday must be Monday, Oct 5, 2026
        assertEquals(DayOfWeek.MONDAY, nextDateTime.dayOfWeek)
        assertEquals(5, nextDateTime.dayOfMonth)
        assertEquals(9, nextDateTime.hour)
    }

    @Test
    fun testWeeklyRepeatCalculatesSevenDaysLater() {
        // Tuesday, Oct 6, 2026 at 15:30
        val tuesday = LocalDateTime.of(2026, 10, 6, 15, 30)
        val scheduledMillis = tuesday.atZone(zoneId).toInstant().toEpochMilli()

        val reminder = Reminder(
            id = 3,
            title = "Weekly Seminar",
            scheduledTimeMillis = scheduledMillis,
            repeatType = RepeatType.WEEKLY.name
        )

        val fromMillis = LocalDateTime.of(2026, 10, 6, 16, 0).atZone(zoneId).toInstant().toEpochMilli()

        val nextTrigger = ReminderScheduler.calculateNextOccurrence(reminder, fromMillis, zoneId)
        val nextDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTrigger), zoneId)

        // Next occurrence must be Tuesday, Oct 13, 2026 at 15:30
        assertEquals(DayOfWeek.TUESDAY, nextDateTime.dayOfWeek)
        assertEquals(13, nextDateTime.dayOfMonth)
        assertEquals(15, nextDateTime.hour)
        assertEquals(30, nextDateTime.minute)
    }

    @Test
    fun testMonthlyRepeatCalculatesNextMonth() {
        // Oct 15, 2026 at 11:00
        val oct15 = LocalDateTime.of(2026, 10, 15, 11, 0)
        val scheduledMillis = oct15.atZone(zoneId).toInstant().toEpochMilli()

        val reminder = Reminder(
            id = 4,
            title = "Monthly Bill",
            scheduledTimeMillis = scheduledMillis,
            repeatType = RepeatType.MONTHLY.name
        )

        val fromMillis = LocalDateTime.of(2026, 10, 15, 12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val nextTrigger = ReminderScheduler.calculateNextOccurrence(reminder, fromMillis, zoneId)
        val nextDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTrigger), zoneId)

        // Next occurrence: Nov 15, 2026 at 11:00
        assertEquals(11, nextDateTime.monthValue)
        assertEquals(15, nextDateTime.dayOfMonth)
        assertEquals(11, nextDateTime.hour)
    }

    @Test
    fun testAdvanceNotificationOffset() {
        val targetDateTime = LocalDateTime.of(2026, 10, 1, 10, 0)
        val scheduledMillis = targetDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val reminder = Reminder(
            id = 5,
            title = "Meeting",
            scheduledTimeMillis = scheduledMillis,
            advanceMinutes = 15 // 15 minutes before
        )

        val triggerMillis = reminder.getTriggerTimeMillis()
        val triggerDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerMillis), zoneId)

        // 10:00 minus 15 min = 09:45
        assertEquals(9, triggerDateTime.hour)
        assertEquals(45, triggerDateTime.minute)
    }

    @Test
    fun testTimetableClassNextOccurrenceCalculation() {
        // Monday (1) and Wednesday (3) at 09:30, 5 minutes advance reminder
        val cls = TimetableClass(
            id = 10,
            subject = "Business Studies",
            startHour = 9,
            startMinute = 30,
            endHour = 10,
            endMinute = 30,
            daysOfWeek = "1,3",
            advanceMinutes = 5
        )

        // Given current time is Monday at 10:00 UTC (Oct 5, 2026)
        val fromMillis = LocalDateTime.of(2026, 10, 5, 10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val triggerMillis = ReminderScheduler.calculateNextClassOccurrence(cls, fromMillis, zoneId)
        val triggerDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerMillis), zoneId)

        // Next class is Wednesday, Oct 7 at 09:30.
        // With 5 minutes advance, trigger should be at 09:25 on Wednesday
        assertEquals(DayOfWeek.WEDNESDAY, triggerDateTime.dayOfWeek)
        assertEquals(7, triggerDateTime.dayOfMonth)
        assertEquals(9, triggerDateTime.hour)
        assertEquals(25, triggerDateTime.minute)
    }
}
