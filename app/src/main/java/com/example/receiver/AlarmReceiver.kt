package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.SmartReminderApp
import com.example.alarm.ReminderScheduler
import com.example.data.model.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = SmartReminderApp.instance
                val soundEnabled = app.settingsRepository.soundEnabled.value
                val vibrationEnabled = app.settingsRepository.vibrationEnabled.value

                when (action) {
                    ReminderScheduler.ACTION_TRIGGER_REMINDER -> {
                        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
                        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)
                        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Reminder"
                        val description = intent.getStringExtra(ReminderScheduler.EXTRA_DESCRIPTION) ?: ""
                        val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false)

                        Log.d(TAG, "Triggered reminder alarm for ID: $reminderId, isSnooze: $isSnooze")

                        // Show notification
                        app.notificationHelper.showReminderNotification(
                            notificationId = notificationId,
                            reminderId = reminderId,
                            title = title,
                            description = description,
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled
                        )

                        // If not snooze, handle repeating rescheduling or one-time completion
                        if (!isSnooze && reminderId > 0) {
                            val reminder = app.repository.getReminderByIdSync(reminderId)
                            if (reminder != null && reminder.isEnabled && !reminder.isCompleted) {
                                if (reminder.getRepeatTypeEnum() != RepeatType.NONE) {
                                    val now = System.currentTimeMillis()
                                    val nextOccurrence = ReminderScheduler.calculateNextOccurrence(reminder, now)
                                    val updatedReminder = reminder.copy(
                                        scheduledTimeMillis = nextOccurrence,
                                        updatedTimestamp = now
                                    )
                                    app.repository.updateReminder(updatedReminder)
                                    // Schedule the next occurrence
                                    app.scheduler.scheduleReminder(updatedReminder)
                                    Log.d(TAG, "Rescheduled repeating reminder $reminderId to $nextOccurrence")
                                } else {
                                    // One-time reminder has triggered; mark as completed
                                    val updatedReminder = reminder.copy(
                                        isCompleted = true,
                                        updatedTimestamp = System.currentTimeMillis()
                                    )
                                    app.repository.updateReminder(updatedReminder)
                                }
                            }
                        }
                    }

                    ReminderScheduler.ACTION_TRIGGER_CLASS -> {
                        val classId = intent.getLongExtra(ReminderScheduler.EXTRA_CLASS_ID, -1L)
                        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)
                        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Class"
                        val description = intent.getStringExtra(ReminderScheduler.EXTRA_DESCRIPTION) ?: ""

                        app.notificationHelper.showClassNotification(
                            notificationId = notificationId,
                            classId = classId,
                            title = title,
                            description = description,
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled
                        )

                        // Reschedule next class occurrence
                        if (classId > 0) {
                            val timetableClass = app.repository.getClassByIdSync(classId)
                            if (timetableClass != null && timetableClass.isEnabled) {
                                app.scheduler.scheduleClass(timetableClass)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in AlarmReceiver onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
