package com.example.neurocalendar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.domain.model.nextOccurrenceOnOrAfter
import java.time.Instant
import java.time.ZoneId
import android.Manifest
import android.content.pm.PackageManager

class EventReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(event: Event) {
        cancel(event.id)
        if (event.reminderMinutesBefore <= 0) return

        val now = System.currentTimeMillis()
        val triggerAtMillis = nextTriggerAtMillis(event, now) ?: return
        if (!canScheduleExactAlarms()) return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            createPendingIntent(
                event,
                triggerAtMillis + event.reminderMinutesBefore * MILLIS_PER_MINUTE,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun requiresExactAlarmAccess(event: Event): Boolean {
        return event.reminderMinutesBefore > 0 &&
            nextTriggerAtMillis(event, System.currentTimeMillis()) != null &&
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

    private fun createPendingIntent(
        event: Event,
        occurrenceStartMillis: Long,
        flags: Int
    ): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            event.id,
            reminderIntent(event.id, event.title, event.reminderMinutesBefore, occurrenceStartMillis),
            flags
        )

    private fun reminderIntent(
        eventId: Int,
        title: String,
        minutesBefore: Int,
        occurrenceStartMillis: Long = 0L
    ) =
        Intent(appContext, EventReminderReceiver::class.java).apply {
            action = ACTION_EVENT_REMINDER
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, title)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
            putExtra(EXTRA_OCCURRENCE_START, occurrenceStartMillis)
        }

    private fun nextTriggerAtMillis(event: Event, nowMillis: Long): Long? {
        val zone = ZoneId.systemDefault()
        val anchorStart = event.getStartTime()
        val anchorDate = anchorStart.toLocalDate()
        val untilDate = event.repeatUntilMillis?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        var occurrenceDate = nextOccurrenceOnOrAfter(
            anchorDate,
            today,
            event.repeatInterval,
            event.repeatUnit,
            untilDate
        )
        while (occurrenceDate != null) {
            val occurrenceStart = occurrenceDate.atTime(anchorStart.toLocalTime())
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            val triggerAtMillis = occurrenceStart -
                event.reminderMinutesBefore * MILLIS_PER_MINUTE
            if (triggerAtMillis > nowMillis) return triggerAtMillis
            occurrenceDate = nextOccurrenceOnOrAfter(
                anchorDate,
                occurrenceDate.plusDays(1),
                event.repeatInterval,
                event.repeatUnit,
                untilDate
            )
        }
        return null
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        const val ACTION_EVENT_REMINDER = "com.example.neurocalendar.EVENT_REMINDER"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
        const val EXTRA_OCCURRENCE_START = "occurrence_start"

        fun notificationsAllowed(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED)
    }
}
