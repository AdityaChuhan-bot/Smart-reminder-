package com.adityachuhan.smartreminder

import android.app.*
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
        val pendingResult = goAsync()
        val title = intent.getStringExtra("title") ?: "Reminder"
        val id = intent.getIntExtra("id", 1)
        val repeat = intent.getStringExtra("repeat") ?: "NONE"

        val open = PendingIntent.getActivity(
            context, 100, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getBroadcast(
            context, id + 10000,
            Intent(context, SnoozeReceiver::class.java)
                .putExtra("id", id).putExtra("title", title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Reminder")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 10 min", snooze)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, n)

        if (repeat != "NONE") {
            CoroutineScope(Dispatchers.IO).launch {
                val reminder = AppDatabase.get(context).reminderDao().get(id.toLong())
                if (reminder != null && reminder.enabled) {
                    val next = reminder.copy(triggerAt = ReminderScheduler.nextOccurrence(System.currentTimeMillis(), reminder.repeat))
                    AppDatabase.get(context).reminderDao().update(next)
                    ReminderScheduler.schedule(context, next)
                }
                pendingResult.finish()
            }
        } else {
            pendingResult.finish()
        }
    }
}
