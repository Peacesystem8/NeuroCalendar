package com.example.neurocalendar.data.repository

import com.example.neurocalendar.data.dao.CalendarDao
import com.example.neurocalendar.data.model.Event
import kotlinx.coroutines.Dispatchers
import com.example.neurocalendar.domain.model.CalendarItem
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository to abstract data sources and handle merging of different item types.
 */
class CalendarRepository(private val dao: CalendarDao) {

    suspend fun saveEvent(event: Event): Event {
        val savedId = dao.insertEvent(event).toInt()
        return event.copy(id = savedId)
    }

    suspend fun getUpcomingReminderEvents(currentTimeMillis: Long): List<Event> =
        dao.getUpcomingReminderEvents(currentTimeMillis)

    /**
     * Fetches all events and tasks for a given day, merges them into the unified domain model,
     * and returns them in a flow sorted by their display time.
     */
    fun getCalendarItemsForDay(date: LocalDate): Flow<List<CalendarItem>> {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextDayStart = date.plusDays(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        return combine(
            dao.getEventsOnDay(dayStart, nextDayStart - 1),
            dao.getTasksOnDay(dayStart)
        ) { events, tasks ->
        // Map raw entities to the unified domain model
        val eventItems = events.mapNotNull { event ->
            CalendarItem.EventItem(
                id = event.id,
                title = event.title,
                description = event.description,
                startTime = event.getStartTime(),
                endTime = event.getEndTime(),
                location = event.location,
                reminderMinutesBefore = event.reminderMinutesBefore
            )
        }

        val taskItems = tasks.mapNotNull { task ->
             CalendarItem.TaskItem(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = java.time.Instant.ofEpochMilli(task.dueDateMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate(),
                isCompleted = task.isCompleted
            )
        }

        // Merge and sort: The combine function emits when either flow updates.
        // We must sort the merged list by displayTime (chronological).
        (eventItems + taskItems).sortedBy { item -> item.displayTime }
        }.flowOn(Dispatchers.IO)
    }
}