package com.example

import android.app.Application
import com.example.alarm.ReminderScheduler
import com.example.data.database.AppDatabase
import com.example.data.repository.ReminderRepository
import com.example.data.repository.SettingsRepository
import com.example.notification.NotificationHelper

class SmartReminderApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ReminderRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var scheduler: ReminderScheduler
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        repository = ReminderRepository(database.reminderDao(), database.timetableDao())
        settingsRepository = SettingsRepository(this)
        scheduler = ReminderScheduler(this)
        notificationHelper = NotificationHelper(this)
    }

    companion object {
        lateinit var instance: SmartReminderApp
            private set
    }
}
