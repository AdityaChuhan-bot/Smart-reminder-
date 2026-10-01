package com.adityachuhan.smartreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adityachuhan.smartreminder.data.Reminder
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST_BASE = 5000

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.enabled) return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("note", reminder.note)
            putExtra("repeat", reminder.repeat)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.cancel(pending)

        val whenAt = if (reminder.triggerAt > System.currentTimeMillis()) {
            reminder.triggerAt
        } else {
            nextOccurrence(reminder)
        }

        if (whenAt <= System.currentTimeMillis()) return

        if (Build.VERSION.SDK_INT >= 31 && alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        }
    }

    fun cancel(context: Context, reminder: Reminder) {
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + reminder.id.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)?.cancel(pending)
    }

    fun nextOccurrence(reminder: Reminder): Long {
        val original = Calendar.getInstance().apply { timeInMillis = reminder.triggerAt }
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, original.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()

        when (reminder.repeat) {
            "DAILY" -> {
                if (c.timeInMillis <= now) {
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            "WEEKDAYS" -> {
                if (c.timeInMillis <= now) {
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            "WEEKLY" -> {
                val targetDay = reminder.repeatDayOfWeek ?: original.get(Calendar.DAY_OF_WEEK)
                c.set(Calendar.DAY_OF_WEEK, targetDay)
                if (c.timeInMillis <= now) {
                    c.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            else -> return reminder.triggerAt
        }
        return c.timeInMillis
    }
}
