package com.adityachuhan.smartreminder

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adityachuhan.smartreminder.data.Reminder
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST_BASE = 5000

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.enabled) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("id", reminder.id.toInt())
            .putExtra("title", reminder.title)
            .putExtra("repeat", reminder.repeat)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_BASE + reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        val whenAt = if (reminder.triggerAt > System.currentTimeMillis()) {
            reminder.triggerAt
        } else {
            nextOccurrence(reminder.triggerAt, reminder.repeat)
        }
        if (whenAt <= System.currentTimeMillis()) return
        if (Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        }
    }

    fun cancel(context: Context, reminder: Reminder) {
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_BASE + reminder.id.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).cancel(pending)
    }

    fun nextOccurrence(from: Long, repeat: String): Long {
        val c = Calendar.getInstance().apply { timeInMillis = from }
        when (repeat) {
            "DAILY" -> c.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> c.add(Calendar.WEEK_OF_YEAR, 1)
            else -> return from
        }
        return c.timeInMillis
    }
}
