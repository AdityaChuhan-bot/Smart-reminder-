package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Reminders", Icons.Default.Notifications)
    data object Timetable : Screen("timetable", "Timetable", Icons.Default.School)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
