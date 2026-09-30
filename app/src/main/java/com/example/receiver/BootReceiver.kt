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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "BootReceiver received action: $action")

        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )

        if (action !in validActions) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = SmartReminderApp.instance
                val now = System.currentTimeMillis()

                // Reschedule all active reminders
                val activeReminders = app.repository.getActiveRemindersSync()
                Log.d(TAG, "Rescheduling ${activeReminders.size} active reminders after $action")

                for (reminder in activeReminders) {
                    val triggerTime = reminder.getTriggerTimeMillis()
                    if (triggerTime > now) {
                        // Future one-time or repeating
                        app.scheduler.scheduleReminder(reminder)
                    } else if (reminder.getRepeatTypeEnum() != RepeatType.NONE) {
                        // Past-due occurrence of a repeating reminder: calculate next occurrence, update DB and schedule
                        val nextOccurrence = ReminderScheduler.calculateNextOccurrence(reminder, now)
                        val updated = reminder.copy(
                            scheduledTimeMillis = nextOccurrence,
                            updatedTimestamp = now
                        )
                        app.repository.updateReminder(updated)
                        app.scheduler.scheduleReminder(updated)
                    }
                }

                // Reschedule all active classes
                val activeClasses = app.repository.getActiveClassesSync()
                Log.d(TAG, "Rescheduling ${activeClasses.size} active classes after $action")
                for (cls in activeClasses) {
                    app.scheduler.scheduleClass(cls)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling reminders in BootReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
