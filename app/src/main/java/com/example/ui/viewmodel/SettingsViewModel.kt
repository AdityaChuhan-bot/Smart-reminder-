package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartReminderApp
import com.example.backup.BackupManager
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val settingsRepo: SettingsRepository = SmartReminderApp.instance.settingsRepository
    private val app = SmartReminderApp.instance

    val soundEnabled = settingsRepo.soundEnabled
    val vibrationEnabled = settingsRepo.vibrationEnabled
    val defaultAdvanceMinutes = settingsRepo.defaultAdvanceMinutes
    val themeMode = settingsRepo.themeMode

    fun setSoundEnabled(enabled: Boolean) = settingsRepo.setSoundEnabled(enabled)
    fun setVibrationEnabled(enabled: Boolean) = settingsRepo.setVibrationEnabled(enabled)
    fun setDefaultAdvanceMinutes(minutes: Int) = settingsRepo.setDefaultAdvanceMinutes(minutes)
    fun setThemeMode(mode: String) = settingsRepo.setThemeMode(mode)

    suspend fun generateBackupJson(): String {
        val reminders = app.repository.allReminders.first()
        val classes = app.repository.allClasses.first()
        return BackupManager.createBackupJson(reminders, classes)
    }

    fun restoreBackup(
        jsonString: String,
        replaceExisting: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val payload = BackupManager.parseBackupJson(jsonString)
                if (payload.reminders.isEmpty() && payload.timetableClasses.isEmpty()) {
                    onComplete(false, "No valid reminders or classes found in backup file.")
                    return@launch
                }

                if (replaceExisting) {
                    // Cancel all existing alarms first
                    val currentReminders = app.repository.allReminders.first()
                    for (r in currentReminders) {
                        app.scheduler.cancelReminder(r)
                    }
                    val currentClasses = app.repository.allClasses.first()
                    for (c in currentClasses) {
                        app.scheduler.cancelClass(c)
                    }
                    app.repository.clearAllData()
                }

                // Insert imported reminders
                for (reminder in payload.reminders) {
                    val id = app.repository.insertReminder(reminder)
                    val inserted = reminder.copy(id = id)
                    if (inserted.isEnabled && !inserted.isCompleted) {
                        app.scheduler.scheduleReminder(inserted)
                    }
                }

                // Insert imported classes
                for (cls in payload.timetableClasses) {
                    val id = app.repository.insertClass(cls)
                    val inserted = cls.copy(id = id)
                    if (inserted.isEnabled) {
                        app.scheduler.scheduleClass(inserted)
                    }
                }

                val msg = "Imported ${payload.reminders.size} reminders and ${payload.timetableClasses.size} classes successfully."
                onComplete(true, msg)
            } catch (e: Exception) {
                onComplete(false, "Failed to parse backup: ${e.localizedMessage ?: "Invalid format"}")
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            val reminders = app.repository.allReminders.first()
            for (r in reminders) {
                app.scheduler.cancelReminder(r)
            }
            val classes = app.repository.allClasses.first()
            for (c in classes) {
                app.scheduler.cancelClass(c)
            }
            app.repository.clearAllData()
            onComplete()
        }
    }
}
