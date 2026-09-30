# Smart Reminder

A lightweight, privacy-first Android reminder application built with **Kotlin, Jetpack Compose, Material 3, Room, and AlarmManager**.

Smart Reminder is designed for dependable local reminders without requiring an account, cloud backend, advertising SDK, analytics service, or API key.

## Highlights

- ⏰ Create reminders with a title, note, date, and time
- 🔁 One-time, daily, and weekly reminder schedules
- 🔔 Android notifications with reminder actions
- 😴 Built-in 10-minute snooze action
- 🔄 Automatic reminder rescheduling after device reboot
- 💾 Local Room database for persistent reminder storage
- 📱 Android 10+ support
- 🎨 Jetpack Compose + Material 3 interface
- 🔒 Offline-first and privacy-focused
- 🚫 No ads, account, backend, analytics, or API key
- 🤖 Automated GitHub Actions APK builds
- 📦 Automatic GitHub Release publishing after a successful build
- 🖼️ Timetable screenshot import with on-device OCR
- 📚 Automatic weekly class reminders from imported timetables

## Download

The latest successful build is published automatically as a **GitHub Release**.

**Releases:** https://github.com/AdityaChuhan-bot/Smart-reminder-/releases

Each release contains the installable debug APK as a downloadable asset.

> Debug APKs are intended for personal/testing use. The project is not currently distributed through Google Play.

## How automatic publishing works

Every push to `main` triggers the workflow:

1. GitHub checks out the source.
2. Java 17 and Gradle 8.9 are configured.
3. The Android debug APK is compiled.
4. The generated APK is verified.
5. The APK is uploaded as a temporary GitHub Actions artifact.
6. A versioned GitHub Release is created automatically.
7. The APK is attached to that release.

Release versions currently follow:

`v1.0.<GitHub Actions run number>`

Manual builds can also be started from the **Actions → Build and Publish Smart Reminder APK → Run workflow** interface.

## Technical stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Minimum Android | Android 10 / API 29 |
| Compile / Target SDK | 35 |
| Persistence | Room 2.6.1 |
| Scheduling | AlarmManager |
| Architecture | ViewModel + StateFlow |
| Build system | Gradle 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.20 |
| CI/CD | GitHub Actions |
| Java | 17 |

## Reminder architecture

Reminder data is stored locally in a Room database. The scheduling layer uses Android's AlarmManager APIs and the notification layer uses NotificationManager.

For devices where exact-alarm access is unavailable, the scheduler can fall back to an inexact idle-aware alarm rather than preventing the reminder from being scheduled.

On Android 13 and newer, the application requests the `POST_NOTIFICATIONS` runtime permission so reminder notifications can be displayed.

After a device reboot, the boot receiver reloads enabled reminders from the local database and schedules them again.

## Permissions

The application uses the following Android permissions:

- `POST_NOTIFICATIONS` — required on Android 13+ for notifications.
- `SCHEDULE_EXACT_ALARM` — used when exact reminder timing is available.
- `RECEIVE_BOOT_COMPLETED` — restores enabled reminder schedules after reboot.

No internet permission is required by the reminder functionality itself.

## Project structure

```text
Smart-reminder-/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/adityachuhan/smartreminder/
│       │   ├── MainActivity.kt
│       │   ├── ReminderReceiver.kt
│       │   ├── ReminderScheduler.kt
│       │   ├── ReminderViewModel.kt
│       │   ├── SnoozeReceiver.kt
│       │   ├── BootReceiver.kt
│       │   └── data/
│       │       ├── AppDatabase.kt
│       │       ├── Reminder.kt
│       │       └── ReminderDao.kt
│       └── res/
├── .github/workflows/
│   └── build-apk.yml
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

## Local development

A local Android/Gradle environment with JDK 17 is recommended.

Build the debug APK with:

```bash
gradle --no-daemon :app:assembleDebug
```

The resulting APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For CI, the repository uses GitHub Actions with JDK 17 and Gradle 8.9.

## Privacy

Smart Reminder is designed around local operation.

- No user account
- No cloud backend
- No advertising SDK
- No analytics SDK
- No third-party AI service
- No API key
- Reminder data is stored locally on the device

## Roadmap

Planned improvements include:

- Timetable mode for class schedules
- Custom timetable screenshot import with on-device OCR
- Custom repeat-day selection
- Calendar and reminder filtering
- JSON export/import
- Reminder editing improvements
- Additional notification actions
- Production/release signing
- Further Android 15/16 UI refinements

## Status

**Active development.**

The project currently focuses on a reliable local reminder foundation, timetable screenshot import, and an automated APK delivery pipeline. Features will be expanded incrementally while keeping the application lightweight and privacy-focused.

## License

No open-source license has been declared yet. Until a license is added to the repository, the source should be treated as **all rights reserved** by the repository owner.
