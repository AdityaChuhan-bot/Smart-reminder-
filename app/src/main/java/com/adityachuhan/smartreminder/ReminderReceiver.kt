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
        val id = intent.getLongExtra("id", 0L)
        val open = PendingIntent.getActivity(context, 100, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(intent.getStringExtra("title") ?: "Reminder")
            .setContentText("Smart Reminder")
            .setContentIntent(open).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        context.getSystemService(NotificationManager::class.java).notify(id.toInt(), n)

        if (id > 0 && intent.getStringExtra("repeat") == "WEEKLY") {
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.get(context).reminderDao().getById(id)?.let {
                    ReminderScheduler.schedule(context, it.copy(triggerAt = System.currentTimeMillis() - 1))
                }
            }
        }
    }
}
