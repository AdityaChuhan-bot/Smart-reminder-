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
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("id", reminder.id).putExtra("title", reminder.title).putExtra("repeat", reminder.repeat)
        val pending = PendingIntent.getBroadcast(context, REQUEST_BASE + reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(pending)
        val whenAt = if (reminder.triggerAt > System.currentTimeMillis()) reminder.triggerAt else nextOccurrence(reminder)
        if (whenAt <= System.currentTimeMillis()) return
        if (Build.VERSION.SDK_INT >= 31 && alarm.canScheduleExactAlarms())
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
    }

    fun cancel(context: Context, reminder: Reminder) {
        val pending = PendingIntent.getBroadcast(context, REQUEST_BASE + reminder.id.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).cancel(pending)
    }

    fun nextOccurrence(reminder: Reminder): Long {
        val original = Calendar.getInstance().apply { timeInMillis = reminder.triggerAt }
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
        c.set(Calendar.MINUTE, original.get(Calendar.MINUTE))
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        when (reminder.repeat) {
            "DAILY" -> if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> {
                val target = reminder.repeatDayOfWeek ?: original.get(Calendar.DAY_OF_WEEK)
                c.set(Calendar.DAY_OF_WEEK, target)
                if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.WEEK_OF_YEAR, 1)
            }
            else -> return reminder.triggerAt
        }
        return c.timeInMillis
    }
}
