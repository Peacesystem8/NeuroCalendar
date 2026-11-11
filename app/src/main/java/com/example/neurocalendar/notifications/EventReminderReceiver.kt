package com.example.neurocalendar.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.neurocalendar.MainActivity
import com.example.neurocalendar.R
import com.example.neurocalendar.data.database.CalendarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra(EventReminderScheduler.EXTRA_EVENT_ID, 0)
        if (eventId == 0) return
        val title = intent.getStringExtra(EventReminderScheduler.EXTRA_EVENT_TITLE)
            ?.takeIf(String::isNotBlank)

        if (title != null && EventReminderScheduler.notificationsAllowed(context)) {
            ensureNotificationChannel(context)

            val minutesBefore = intent.getIntExtra(EventReminderScheduler.EXTRA_MINUTES_BEFORE, 0)
            val notificationSound =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val contentIntent = PendingIntent.getActivity(
                context,
                eventId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_event_reminder)
                .setContentTitle(title)
                .setContentText("Starts in $minutesBefore minutes.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$title starts in $minutesBefore minutes.")
                )
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(notificationSound)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(eventId, notification)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CalendarDatabase.getInstance(context).calendarDao()
                    .getEventById(eventId)
                    ?.let { EventReminderScheduler(context).schedule(it) }
            } catch (error: Exception) {
                Log.e("EventReminderReceiver", "Unable to schedule the next event reminder", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Event reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "One notification with the device's notification sound before an event."
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "event_reminders_v3"
    }
}
