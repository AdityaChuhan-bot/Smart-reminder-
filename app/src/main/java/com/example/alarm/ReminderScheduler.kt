package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Reminder
import com.example.data.model.RepeatType
import com.example.data.model.TimetableClass
import com.example.receiver.AlarmReceiver
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules the next trigger alarm for a reminder.
     */
    fun scheduleReminder(reminder: Reminder) {
        if (!reminder.isEnabled || reminder.isCompleted) {
            cancelReminder(reminder)
            return
        }

        val now = System.currentTimeMillis()
        var nextOccurrence = reminder.scheduledTimeMillis
        val advanceOffset = reminder.advanceMinutes * 60 * 1000L

        // If repeating and initial time is in the past, calculate next occurrence
        if (nextOccurrence - advanceOffset <= now && reminder.getRepeatTypeEnum() != RepeatType.NONE) {
            nextOccurrence = calculateNextOccurrence(reminder, now)
        }

        val triggerTime = nextOccurrence - advanceOffset
        if (triggerTime <= now) {
            Log.d(TAG, "Trigger time $triggerTime is already in the past for reminder ${reminder.id}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_NOTIFICATION_ID, reminder.notificationId)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_DESCRIPTION, reminder.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerTime, pendingIntent)
        Log.d(TAG, "Scheduled reminder ${reminder.id} ('${reminder.title}') at $triggerTime")
    }

    /**
     * Cancels an existing alarm for a reminder.
     */
    fun cancelReminder(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder ${reminder.id}")
        }
    }

    /**
     * Schedules a snooze alarm for the specified duration.
     */
    fun scheduleSnooze(
        reminderId: Long,
        notificationId: Int,
        title: String,
        description: String,
        snoozeMinutes: Int
    ) {
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val snoozeNotificationId = notificationId + 500000

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_NOTIFICATION_ID, snoozeNotificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerTime, pendingIntent)
        Log.d(TAG, "Scheduled snooze for reminder $reminderId in $snoozeMinutes mins at $triggerTime")
    }

    /**
     * Schedules the next occurrence alarm for a timetable class.
     */
    fun scheduleClass(timetableClass: TimetableClass) {
        if (!timetableClass.isEnabled) {
            cancelClass(timetableClass)
            return
        }

        val now = System.currentTimeMillis()
        val nextOccurrence = calculateNextClassOccurrence(timetableClass, now)
        if (nextOccurrence <= now) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CLASS
            putExtra(EXTRA_CLASS_ID, timetableClass.id)
            putExtra(EXTRA_NOTIFICATION_ID, timetableClass.notificationId)
            putExtra(EXTRA_TITLE, "Class: ${timetableClass.subject}")
            val desc = buildString {
                if (timetableClass.room.isNotBlank()) append("Room: ${timetableClass.room} ")
                if (timetableClass.teacher.isNotBlank()) append("• ${timetableClass.teacher} ")
                append("(${timetableClass.getFormattedTimeRange()})")
            }
            putExtra(EXTRA_DESCRIPTION, desc.trim())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timetableClass.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(nextOccurrence, pendingIntent)
        Log.d(TAG, "Scheduled class ${timetableClass.id} (${timetableClass.subject}) at $nextOccurrence")
    }

    fun cancelClass(timetableClass: TimetableClass) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CLASS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timetableClass.notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to schedule alarm", fallbackEx)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scheduling alarm", e)
        }
    }

    companion object {
        const val TAG = "ReminderScheduler"
        const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
        const val ACTION_TRIGGER_CLASS = "com.example.action.TRIGGER_CLASS"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        /**
         * Calculates the next occurrence of a repeating reminder after `fromTimeMillis`.
         * Pure function, easily testable via unit tests.
         */
        fun calculateNextOccurrence(
            reminder: Reminder,
            fromTimeMillis: Long,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val advanceOffset = reminder.advanceMinutes * 60 * 1000L
            val scheduledDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(reminder.scheduledTimeMillis),
                zoneId
            )
            val fromDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(fromTimeMillis + advanceOffset),
                zoneId
            )

            var candidate = scheduledDateTime

            when (reminder.getRepeatTypeEnum()) {
                RepeatType.NONE -> {
                    return reminder.scheduledTimeMillis
                }
                RepeatType.DAILY -> {
                    // Set candidate time to today's date with reminder's hour and minute
                    candidate = fromDateTime.withHour(scheduledDateTime.hour)
                        .withMinute(scheduledDateTime.minute)
                        .withSecond(0)
                        .withNano(0)
                    if (!candidate.isAfter(fromDateTime)) {
                        candidate = candidate.plusDays(1)
                    }
                }
                RepeatType.WEEKDAYS -> {
                    candidate = fromDateTime.withHour(scheduledDateTime.hour)
                        .withMinute(scheduledDateTime.minute)
                        .withSecond(0)
                        .withNano(0)
                    if (!candidate.isAfter(fromDateTime)) {
                        candidate = candidate.plusDays(1)
                    }
                    while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
                        candidate = candidate.plusDays(1)
                    }
                }
                RepeatType.WEEKLY -> {
                    // Match the day of week of the original scheduled date
                    val targetDay = scheduledDateTime.dayOfWeek
                    candidate = fromDateTime.withHour(scheduledDateTime.hour)
                        .withMinute(scheduledDateTime.minute)
                        .withSecond(0)
                        .withNano(0)
                    if (candidate.dayOfWeek == targetDay && candidate.isAfter(fromDateTime)) {
                        // Candidate is today and in future
                    } else {
                        candidate = candidate.with(TemporalAdjusters.next(targetDay))
                    }
                }
                RepeatType.MONTHLY -> {
                    val targetDayOfMonth = scheduledDateTime.dayOfMonth
                    val targetMonth = fromDateTime.year to fromDateTime.monthValue
                    var year = targetMonth.first
                    var month = targetMonth.second

                    var validDay = targetDayOfMonth.coerceAtMost(
                        java.time.YearMonth.of(year, month).lengthOfMonth()
                    )
                    candidate = LocalDateTime.of(
                        year,
                        month,
                        validDay,
                        scheduledDateTime.hour,
                        scheduledDateTime.minute,
                        0,
                        0
                    )

                    if (!candidate.isAfter(fromDateTime)) {
                        val nextMonth = candidate.plusMonths(1)
                        year = nextMonth.year
                        month = nextMonth.monthValue
                        validDay = targetDayOfMonth.coerceAtMost(
                            java.time.YearMonth.of(year, month).lengthOfMonth()
                        )
                        candidate = LocalDateTime.of(
                            year,
                            month,
                            validDay,
                            scheduledDateTime.hour,
                            scheduledDateTime.minute,
                            0,
                            0
                        )
                    }
                }
                RepeatType.CUSTOM -> {
                    val daysList = if (reminder.repeatDays.isNotBlank()) {
                        reminder.repeatDays.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .map { DayOfWeek.of(it) }
                            .toSet()
                    } else {
                        setOf(scheduledDateTime.dayOfWeek)
                    }

                    if (daysList.isEmpty()) {
                        return candidate.atZone(zoneId).toInstant().toEpochMilli()
                    }

                    candidate = fromDateTime.withHour(scheduledDateTime.hour)
                        .withMinute(scheduledDateTime.minute)
                        .withSecond(0)
                        .withNano(0)

                    if (candidate.isAfter(fromDateTime) && candidate.dayOfWeek in daysList) {
                        // Today matches and time is in the future
                    } else {
                        var found = false
                        var daysAhead = 1L
                        while (!found && daysAhead <= 14L) {
                            val nextCandidate = candidate.plusDays(daysAhead)
                            if (nextCandidate.dayOfWeek in daysList) {
                                candidate = nextCandidate
                                found = true
                            } else {
                                daysAhead++
                            }
                        }
                    }
                }
            }

            return candidate.atZone(zoneId).toInstant().toEpochMilli()
        }

        /**
         * Calculates next trigger time for a class schedule.
         */
        fun calculateNextClassOccurrence(
            timetableClass: TimetableClass,
            fromTimeMillis: Long,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val advanceOffset = timetableClass.advanceMinutes * 60 * 1000L
            val fromDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(fromTimeMillis + advanceOffset),
                zoneId
            )
            val allowedDays = timetableClass.getDaysList().map { DayOfWeek.of(it) }.toSet()
            if (allowedDays.isEmpty()) return -1L

            var candidate = fromDateTime
                .withHour(timetableClass.startHour)
                .withMinute(timetableClass.startMinute)
                .withSecond(0)
                .withNano(0)

            if (candidate.isAfter(fromDateTime) && candidate.dayOfWeek in allowedDays) {
                // Today matches and class is in future
            } else {
                var found = false
                var daysAhead = 1L
                while (!found && daysAhead <= 14L) {
                    val nextCandidate = candidate.plusDays(daysAhead)
                    if (nextCandidate.dayOfWeek in allowedDays) {
                        candidate = nextCandidate
                        found = true
                    } else {
                        daysAhead++
                    }
                }
            }

            val classStartMillis = candidate.atZone(zoneId).toInstant().toEpochMilli()
            return classStartMillis - advanceOffset
        }
    }
}
