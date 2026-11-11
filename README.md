# NeuroCalendar

NeuroCalendar is a simple daily calendar for planning events and receiving a gentle reminder before they begin. Its warm, nature-inspired interface keeps the day's schedule easy to scan.

## Features

- Browse the calendar one day at a time.
- Create and edit events with a title, start and end times, optional notes, and location.
- Enter times in 12-hour AM/PM or 24-hour format.
- Choose a one-time event reminder 5, 10, 15, or 30 minutes before the event.
- View the day's scheduled events and any tasks already stored in the calendar.
- Store calendar data on the device with Room.

## Requirements

- Android Studio
- Android SDK 37
- Android device or emulator running Android 7.0 (API 24) or later

## Build

Open the project in Android Studio and run the `app` configuration, or build a debug APK from a terminal:

**Windows PowerShell**

```powershell
.\gradlew.bat :app:assembleDebug
```

**macOS or Linux**

```shell
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Event reminders

Enable app notifications to receive reminders. Android may also require **Alarms & reminders** access for punctual delivery. Reminder timing can be affected by device settings such as Do Not Disturb.

## Technology

Kotlin, Jetpack Compose, Material 3, Room, and Android alarms and notifications.

## AI disclosure

This app was generated using local AI in Android Studio.
