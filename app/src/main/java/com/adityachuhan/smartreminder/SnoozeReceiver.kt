package com.adityachuhan.smartreminder

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val id = intent.getIntExtra("id", 1)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val show = Intent(context, MainActivity::class.java)
        val content = PendingIntent.getActivity(context, 200, show, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notificationIntent = Intent(context, ReminderReceiver::class.java)
            .putExtra("id", id).putExtra("title", title).putExtra("repeat", "NONE")
        val pending = PendingIntent.getBroadcast(context, id + 20000, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 60 * 1000, pending)
        context.getSystemService(NotificationManager::class.java).cancel(id)
    }
}
