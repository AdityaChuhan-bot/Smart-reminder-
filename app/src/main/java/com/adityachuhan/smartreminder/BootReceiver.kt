package com.adityachuhan.smartreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adityachuhan.smartreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED) {
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(context).reminderDao().getEnabled().forEach { reminder ->
                    ReminderScheduler.schedule(context, reminder)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
