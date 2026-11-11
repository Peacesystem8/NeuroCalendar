package com.example.neurocalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.neurocalendar.data.database.CalendarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!EventReminderScheduler.notificationsAllowed(context)) return@launch
                val events = CalendarDatabase.getInstance(context)
                    .calendarDao()
                    .getUpcomingReminderEvents(System.currentTimeMillis())
                val scheduler = EventReminderScheduler(context)
                if (events.isNotEmpty() && !scheduler.canScheduleExactAlarms()) {
                    Log.w("ReminderReschedule", "Exact alarm access is disabled; reminders were not restored")
                    return@launch
                }
                events.forEach(scheduler::schedule)
            } catch (error: Exception) {
                Log.e("ReminderReschedule", "Unable to restore event reminders", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
