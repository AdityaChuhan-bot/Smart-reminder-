package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.SmartReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = SmartReminderApp.instance
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

                // Always dismiss current notification
                if (notificationId != 0) {
                    app.notificationHelper.cancelNotification(notificationId)
                }

                when (action) {
                    ACTION_MARK_DONE -> {
                        if (reminderId > 0) {
                            val reminder = app.repository.getReminderByIdSync(reminderId)
                            if (reminder != null) {
                                val updated = reminder.copy(
                                    isCompleted = true,
                                    updatedTimestamp = System.currentTimeMillis()
                                )
                                app.repository.updateReminder(updated)
                                app.scheduler.cancelReminder(reminder)
                                Log.d(TAG, "Marked reminder $reminderId as done from notification")
                            }
                        }
                    }

                    ACTION_SNOOZE -> {
                        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""

                        app.scheduler.scheduleSnooze(
                            reminderId = reminderId,
                            notificationId = notificationId,
                            title = title,
                            description = description,
                            snoozeMinutes = snoozeMinutes
                        )
                        Log.d(TAG, "Snoozed reminder $reminderId for $snoozeMinutes minutes")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in NotificationActionReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_MARK_DONE = "com.example.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.example.action.SNOOZE"
        const val EXTRA_REMINDER_ID = "extra_action_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "extra_action_notification_id"
        const val EXTRA_TITLE = "extra_action_title"
        const val EXTRA_DESCRIPTION = "extra_action_description"
        const val EXTRA_SNOOZE_MINUTES = "extra_action_snooze_minutes"
    }
}
