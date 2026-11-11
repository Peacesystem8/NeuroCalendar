package com.example.neurocalendar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.neurocalendar.data.model.Event

class EventReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(event: Event) {
        cancel(event.id)
        if (event.reminderMinutesBefore <= 0) return

        val triggerAtMillis = event.startTimeMillis -
            event.reminderMinutesBefore * MILLIS_PER_MINUTE
        if (triggerAtMillis <= System.currentTimeMillis()) return
        if (!canScheduleExactAlarms()) return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            createPendingIntent(event, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun requiresExactAlarmAccess(event: Event): Boolean {
        val triggerAtMillis = event.startTimeMillis -
            event.reminderMinutesBefore * MILLIS_PER_MINUTE
        return event.reminderMinutesBefore > 0 &&
            triggerAtMillis > System.currentTimeMillis() &&
            !canScheduleExactAlarms()
    }

    fun cancel(eventId: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            eventId,
            reminderIntent(eventId, "", 0),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(event: Event, flags: Int): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            event.id,
            reminderIntent(event.id, event.title, event.reminderMinutesBefore),
            flags
        )

    private fun reminderIntent(eventId: Int, title: String, minutesBefore: Int) =
        Intent(appContext, EventReminderReceiver::class.java).apply {
            action = ACTION_EVENT_REMINDER
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, title)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        const val ACTION_EVENT_REMINDER = "com.example.neurocalendar.EVENT_REMINDER"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"

        fun notificationsAllowed(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED)
    }
}
