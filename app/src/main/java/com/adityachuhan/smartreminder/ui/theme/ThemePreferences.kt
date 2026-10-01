package com.adityachuhan.smartreminder.ui.theme

import android.content.Context

enum class AppThemeMode(val title: String, val subtitle: String) {
    SYSTEM("System Default", "Follows Android system settings"),
    LIGHT("Light Mode", "Bright, clean daytime interface"),
    DARK("Dark Mode", "Deep slate & navy dark theme"),
    AMOLED("AMOLED Black", "Pure #000000 black for OLED battery saving")
}

object ThemePreferences {
    private const val PREFS_NAME = "smart_reminder_theme_prefs"
    private const val KEY_THEME = "selected_theme_mode"

    fun getThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}
