# Smart Reminder

A lightweight, privacy-first Android reminder, timetable, and to-do application built with **Kotlin, Jetpack Compose, Material 3, Room, and AlarmManager**.

Smart Reminder is designed for dependable local notifications and task tracking without requiring an account, cloud backend, advertising SDK, analytics service, or API key.

## Highlights

### ⏰ Smart Reminders & Alarms
- **Alarm-grade precision**: Scheduled using Android's `AlarmManager` with idle/Doze-safe execution (`setExactAndAllowWhileIdle`).
- **Flexible recurrence**: One-time, daily, weekdays (Mon–Fri), and weekly schedules with custom day-of-week selection.
- **Actionable notifications**: High-priority heads-up alerts with direct **+10m Snooze** and **+1h Snooze** actions.
- **Reboot & timezone resilience**: Automatic rescheduling via `BootReceiver` across device reboots, app updates, and clock adjustments.
- **Reminder management**: Full editing, relative countdown badges ("In 25m", "Due Tomorrow", "Overdue"), and search/filter chips (`All`, `Today`, `Active`, `Repeating`, `One-time`).

### 📝 To-Do List & Task Manager
- **Task tracking**: Create and manage tasks with titles, detailed notes, priority levels, and due dates.
- **Priorities & categories**: Color-coded badges for **High**, **Normal**, and **Low** priorities, plus custom categories (**Study**, **Assignment**, **Personal**, **Work**, **Errands**).
- **Progress overview**: Live task completion bar showing completed vs. pending tasks with progress percentage.
- **Quick task capture**: Inline top input bar for frictionless, instant task creation.
- **1-Tap Alarm conversion**: Instantly turn any to-do task into an exact alarm reminder with prefilled details.
- **Task cleanup**: Batch "Clear Completed" action to keep your task list tidy.

### 📚 Timetable OCR Scanner
- **On-device screenshot import**: Extract class schedules directly from timetable screenshots using Google ML Kit Text Recognition.
- **Zero cloud upload**: 100% private on-device machine learning OCR processing.
- **Smart parsing**: Automatically detects subject names, weekdays, and start times.
- **Lead-time reminders**: Configurable advance alerts (at class time, 5m, 10m, 15m, or 30m before each class).
- **Weekly schedule view**: Daily timeline view (`Mon` – `Sun`) showing scheduled classes and class counts.

### 🔒 Privacy & Local Storage
- **100% Offline**: All data stays securely in local SQLite Room database.
- **No ads, accounts, or trackers**: Zero analytics, telemetry, or external API requirements.

### 🖤 Pure AMOLED Pitch Black Theme
- **True #000000 background**: Completely turns off black pixels on OLED/AMOLED displays for maximum battery savings and infinite contrast.
- **4 theme choices**: System Default, Light Mode, Dark Mode, and AMOLED Black.
- **Persistent selection**: Display preference is saved across app launches.

---

## Download

The latest successful build is published automatically as a **GitHub Release**.

**Releases:** https://github.com/AdityaChuhan-bot/Smart-reminder-/releases

Each release contains the installable debug APK as a downloadable asset.

> Debug APKs are intended for personal/testing use. The project is not currently distributed through Google Play.

---

## How Automatic Publishing Works

Every push to `main` triggers the GitHub Actions workflow:

1. Checks out the repository source.
2. Configures Java 17 (Temurin) and Gradle.
3. Compiles and runs all unit tests (`testDebugUnitTest`).
4. Builds the Android debug APK (`assembleDebug`).
5. Uploads the APK as an Actions artifact.
6. Automatically publishes a versioned GitHub Release with the APK attached.

Release tag format: `v1.0.<GitHub Actions run number>`

Manual builds can also be triggered via **Actions → Build and Publish Smart Reminder APK → Run workflow**.

---

## Technical Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.1.20 |
| UI Framework | Jetpack Compose (BOM 2024.12.01) |
| Design System | Material 3 (M3) |
| Minimum Android | Android 10 (API 29) |
| Target / Compile SDK | Android 15 (API 35) |
| Local Database | Room 2.6.1 (with KSP) |
| Task Scheduling | AlarmManager (RTC_WAKEUP) |
| On-Device OCR | Google ML Kit Text Recognition 16.0.1 |
| Architecture | MVVM + Repository + StateFlow |
| Build System | Gradle 8.9 + AGP 8.7.3 |
| CI / CD | GitHub Actions |
| JDK | OpenJDK 17 |

---

## Architecture & Permissions

### Permissions Used
- `POST_NOTIFICATIONS`: Required on Android 13+ (API 33+) to post reminder notifications.
- `SCHEDULE_EXACT_ALARM`: Enables exact alarm delivery during Doze mode on Android 12+ (API 31+).
- `RECEIVE_BOOT_COMPLETED`: Restores all enabled reminders and timetable alarms upon device reboot.

*No internet permission is required or declared.*

---

## Project Structure

```text
Smart-reminder-/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/adityachuhan/smartreminder/
│       │   │   ├── MainActivity.kt               # Main 4-tab scaffold (Reminders, To-Do, Timetable, Settings)
│       │   │   ├── ReminderReceiver.kt           # BroadcastReceiver for alarms & snooze actions
│       │   │   ├── ReminderScheduler.kt          # AlarmManager scheduler & recurrence calculation
│       │   │   ├── ReminderViewModel.kt          # StateFlow manager for reminders
│       │   │   ├── TodoViewModel.kt              # StateFlow manager for to-do items
│       │   │   ├── TimetableViewModel.kt         # Timetable import & weekly scheduling
│       │   │   ├── TimetableOcr.kt               # On-device ML Kit image OCR parser
│       │   │   ├── TimetableImportDialog.kt      # OCR review & class selection dialog
│       │   │   ├── BootReceiver.kt               # Reboot & timezone recovery receiver
│       │   │   ├── data/
│       │   │   │   ├── AppDatabase.kt            # Room database definition with migrations
│       │   │   │   ├── Reminder.kt               # Reminder Room entity
│       │   │   │   ├── ReminderDao.kt            # CRUD & query DAO for reminders
│       │   │   │   ├── TodoItem.kt               # To-Do task Room entity
│       │   │   │   └── TodoDao.kt                # CRUD & query DAO for to-do tasks
│       │   │   └── ui/
│       │   │       ├── components/
│       │   │       │   ├── HeroCard.kt           # Gradient dashboard hero banner
│       │   │       │   ├── StatCard.kt           # Interactive stat filter cards
│       │   │       │   ├── ReminderCard.kt       # Reminder card with relative time & snooze
│       │   │       │   ├── TodoItemCard.kt       # Task item with priority, due date & alarm action
│       │   │       │   ├── AddReminderDialog.kt  # Add/edit reminder with presets & custom repeat
│       │   │       │   └── AddEditTodoDialog.kt  # Add/edit to-do item dialog
│       │   │       └── theme/
│       │   │           ├── Color.kt              # Material 3 light/dark palette
│       │   │           └── Theme.kt              # Central Compose theme
│       │   └── res/
│       └── test/java/com/adityachuhan/smartreminder/
│           ├── ReminderSchedulerTest.kt          # Unit tests for repeat calculations
│           └── TodoItemTest.kt                   # Unit tests for To-Do item model & toggle
├── .github/workflows/
│   └── build-apk.yml                             # Automated APK build & release workflow
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

---

## Local Development

To build the debug APK locally:

```bash
gradle :app:assembleDebug
```

To run unit tests:

```bash
gradle :app:testDebugUnitTest
```

Output APK location:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Privacy Pledge

Smart Reminder operates strictly on-device:
- No telemetry or tracking
- No background network connections
- No user accounts or registrations
- All reminders, tasks, and timetable images are processed locally

---

## Status

**Active development.** The project provides a dependable local reminder system, on-device timetable extraction, task management, and an automated continuous delivery release pipeline.

## License

All rights reserved by the repository owner.
