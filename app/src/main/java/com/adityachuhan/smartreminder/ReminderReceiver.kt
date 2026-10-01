package com.adityachuhan.smartreminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.adityachuhan.smartreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val id = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val note = intent.getStringExtra("note") ?: ""
        val repeat = intent.getStringExtra("repeat") ?: "NONE"

        if (action == ACTION_SNOOZE) {
            context.getSystemService(NotificationManager::class.java)?.cancel(id.toInt())
            val snoozeMinutes = intent.getIntExtra("snooze_minutes", 10)
            val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.get(context).reminderDao()
                val existing = db.getById(id)
                if (existing != null) {
                    val snoozed = existing.copy(triggerAt = snoozeTime, enabled = true)
                    db.update(snoozed)
                    ReminderScheduler.schedule(context, snoozed)
                }
            }
            return
        }

        val openIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent10m = Intent(context, ReminderReceiver::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra("id", id)
            putExtra("title", title)
            putExtra("note", note)
            putExtra("repeat", repeat)
            putExtra("snooze_minutes", 10)
        }
        val snoozePending10m = PendingIntent.getBroadcast(
            context,
            (id + 20000).toInt(),
            snoozeIntent10m,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent1h = Intent(context, ReminderReceiver::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra("id", id)
            putExtra("title", title)
            putExtra("note", note)
            putExtra("repeat", repeat)
            putExtra("snooze_minutes", 60)
        }
        val snoozePending1h = PendingIntent.getBroadcast(
            context,
            (id + 30000).toInt(),
            snoozeIntent1h,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (note.isNotBlank()) note else "Smart Reminder alert"

        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 10m", snoozePending10m)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 1h", snoozePending1h)
            .build()

        context.getSystemService(NotificationManager::class.java)?.notify(id.toInt(), notification)

        // Reschedule recurring reminders for their next occurrence
        if (id > 0 && repeat != "NONE") {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.get(context).reminderDao()
                db.getById(id)?.let { current ->
                    val nextTime = ReminderScheduler.nextOccurrence(current)
                    val updated = current.copy(triggerAt = nextTime)
                    db.update(updated)
                    ReminderScheduler.schedule(context, updated)
                }
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.adityachuhan.smartreminder.ACTION_SNOOZE"
    }
}
