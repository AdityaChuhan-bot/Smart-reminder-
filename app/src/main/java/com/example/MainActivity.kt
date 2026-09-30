package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notification.NotificationHelper
import com.example.ui.calendar.CalendarScreen
import com.example.ui.common.PermissionRationaleDialog
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Screen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.timetable.TimetableScreen
import com.example.ui.viewmodel.ReminderViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TimetableViewModel

class MainActivity : ComponentActivity() {

    private val reminderViewModel by viewModels<ReminderViewModel>()
    private val timetableViewModel by viewModels<TimetableViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched from a notification
        val targetReminderId = intent?.getLongExtra(NotificationHelper.EXTRA_NAV_REMINDER_ID, -1L) ?: -1L
        val targetClassId = intent?.getLongExtra(NotificationHelper.EXTRA_NAV_CLASS_ID, -1L) ?: -1L

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val defaultAdvanceMinutes by settingsViewModel.defaultAdvanceMinutes.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                MainAppScaffold(
                    reminderViewModel = reminderViewModel,
                    timetableViewModel = timetableViewModel,
                    settingsViewModel = settingsViewModel,
                    defaultAdvanceMinutes = defaultAdvanceMinutes,
                    initialReminderId = targetReminderId,
                    initialClassId = targetClassId
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    reminderViewModel: ReminderViewModel,
    timetableViewModel: TimetableViewModel,
    settingsViewModel: SettingsViewModel,
    defaultAdvanceMinutes: Int,
    initialReminderId: Long,
    initialClassId: Long
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Notification Permission Handling for Android 13+
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                showPermissionRationale = true
            }
        }
    }

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onConfirm = {
                showPermissionRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { showPermissionRationale = false }
        )
    }

    val screens = listOf(
        Screen.Home,
        Screen.Timetable,
        Screen.Calendar,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (initialClassId > 0) Screen.Timetable.route else Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = reminderViewModel,
                    defaultAdvanceMinutes = defaultAdvanceMinutes
                )
            }
            composable(Screen.Timetable.route) {
                TimetableScreen(viewModel = timetableViewModel)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    reminderViewModel = reminderViewModel,
                    timetableViewModel = timetableViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
