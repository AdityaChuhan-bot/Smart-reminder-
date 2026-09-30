package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_reminder_settings", Context.MODE_PRIVATE)

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _defaultAdvanceMinutes = MutableStateFlow(prefs.getInt(KEY_ADVANCE, 0))
    val defaultAdvanceMinutes: StateFlow<Int> = _defaultAdvanceMinutes.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun setDefaultAdvanceMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_ADVANCE, minutes).apply()
        _defaultAdvanceMinutes.value = minutes
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_SOUND = "key_sound_enabled"
        private const val KEY_VIBRATION = "key_vibration_enabled"
        private const val KEY_ADVANCE = "key_default_advance_minutes"
        private const val KEY_THEME = "key_theme_mode"
    }
}
