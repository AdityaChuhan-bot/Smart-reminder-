package com.adityachuhan.smartreminder

import com.adityachuhan.smartreminder.data.Reminder
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {

    @Test
    fun testOneTimeReminderNextOccurrenceReturnsSameTime() {
        val triggerTime = System.currentTimeMillis() + 100000L
        val reminder = Reminder(
            id = 1,
            title = "Test Reminder",
            triggerAt = triggerTime,
            repeat = "NONE"
        )
        val next = ReminderScheduler.nextOccurrence(reminder)
        assertEquals(triggerTime, next)
    }

    @Test
    fun testDailyRepeatInPastAdvancesToFuture() {
        val calPast = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val reminder = Reminder(
            id = 2,
            title = "Daily Past Reminder",
            triggerAt = calPast.timeInMillis,
            repeat = "DAILY"
        )
        val next = ReminderScheduler.nextOccurrence(reminder)
        assertTrue("Next occurrence should be in the future", next > System.currentTimeMillis())

        val calNext = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(14, calNext.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calNext.get(Calendar.MINUTE))
    }

    @Test
    fun testWeekdaysRepeatSkipsWeekends() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val reminder = Reminder(
            id = 3,
            title = "Weekday Reminder",
            triggerAt = cal.timeInMillis,
            repeat = "WEEKDAYS"
        )
        val next = ReminderScheduler.nextOccurrence(reminder)
        val calNext = Calendar.getInstance().apply { timeInMillis = next }
        val dayOfWeek = calNext.get(Calendar.DAY_OF_WEEK)
        assertTrue(
            "Weekday repeat should not land on Saturday or Sunday",
            dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
        )
    }

    @Test
    fun testWeeklyRepeatAdvancesToFutureTargetDay() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -5)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val reminder = Reminder(
            id = 4,
            title = "Weekly Reminder",
            triggerAt = cal.timeInMillis,
            repeat = "WEEKLY",
            repeatDayOfWeek = Calendar.MONDAY
        )
        val next = ReminderScheduler.nextOccurrence(reminder)
        val calNext = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.MONDAY, calNext.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, calNext.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, calNext.get(Calendar.MINUTE))
        assertTrue("Next weekly trigger should be strictly in the future", next > System.currentTimeMillis())
    }
}
