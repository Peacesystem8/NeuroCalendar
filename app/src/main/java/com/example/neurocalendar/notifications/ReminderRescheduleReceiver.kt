package com.example.neurocalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.neurocalendar.data.database.CalendarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!EventReminderScheduler.notificationsAllowed(context)) return@launch
                val dao = CalendarDatabase.getInstance(context).calendarDao()
                val zone = ZoneId.systemDefault()
                val today = Instant.now().atZone(zone).toLocalDate()
                val todayStartMillis = today
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                val events = dao.getUpcomingReminderEvents(
                    System.currentTimeMillis(),
                    todayStartMillis
                )
                val tasks = dao.getUpcomingTaskReminders(todayStartMillis)
                val scheduler = EventReminderScheduler(context)
                val taskScheduler = TaskReminderScheduler(context)
                if ((events.isNotEmpty() || tasks.isNotEmpty()) &&
                    (!scheduler.canScheduleExactAlarms() ||
                        !taskScheduler.canScheduleExactAlarms())
                ) {
                    Log.w("ReminderReschedule", "Exact alarm access is disabled; reminders were not restored")
                    return@launch
                }
                events.forEach(scheduler::schedule)
                tasks.forEach(taskScheduler::schedule)
            } catch (error: Exception) {
                Log.e("ReminderReschedule", "Unable to restore event reminders", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
