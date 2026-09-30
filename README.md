# Smart Reminder

Lightweight, offline-first Android reminder app.

## Current status
This repository contains the stable Android project foundation, notification handling, and automated APK build. Persistent reminder scheduling is the next feature layer.

## Features
- Kotlin + Jetpack Compose + Material 3
- Android 10+ support
- Local/offline architecture
- Android 13+ notification permission handling
- Notification receiver and reboot receiver foundations
- GitHub Actions debug APK build

## Build
GitHub Actions uses JDK 17 and Gradle 8.9.
APK: app/build/outputs/apk/debug/app-debug.apk

## Permissions
POST_NOTIFICATIONS for Android 13+ notifications.
SCHEDULE_EXACT_ALARM for exact user-selected times.
RECEIVE_BOOT_COMPLETED for reboot restoration.

## Privacy
No account, backend, advertising SDK, analytics service, or API key is required.

## Roadmap
1. Stable APK build
2. Room reminder storage
3. Add/edit/delete reminders
4. AlarmManager scheduling
5. Repeat rules
6. Snooze/actions
7. Timetable
8. Import/export
