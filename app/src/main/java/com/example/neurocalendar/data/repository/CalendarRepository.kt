package com.example.neurocalendar.data.repository

import com.example.neurocalendar.data.dao.CalendarDao
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.model.Task
import com.example.neurocalendar.data.model.TaskOccurrenceCompletion
import kotlinx.coroutines.Dispatchers
import com.example.neurocalendar.domain.model.CalendarItem
import com.example.neurocalendar.domain.model.occursOnDate
import kotlinx.coroutines.flow.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CleanupResult(
    val completedTasksDeleted: Int,
    val recurringOccurrencesDismissed: Int,
    val pastEventsDeleted: Int,
    val completedTaskIds: List<Int>,
    val pastEventIds: List<Int>
)

/**
 * Repository to abstract data sources and handle merging of different item types.
 */
class CalendarRepository(private val dao: CalendarDao) {

    suspend fun saveEvent(event: Event): Event {
        val savedId = dao.insertEvent(event).toInt()
        return event.copy(id = savedId)
    }

    suspend fun saveTask(task: Task): Task {
        val savedId = dao.insertTask(task).toInt()
        return task.copy(id = savedId)
    }

    suspend fun getUpcomingTaskReminders(todayStartMillis: Long): List<Task> =
        dao.getUpcomingTaskReminders(todayStartMillis)

    suspend fun getUpcomingReminderEvents(currentTimeMillis: Long): List<Event> =
        dao.getUpcomingReminderEvents(
            currentTimeMillis,
            LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

    suspend fun clearCompletedTasksAndPastEvents(currentTimeMillis: Long): CleanupResult {
        val todayStartMillis = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val pastEventIds = dao.getPastEventIds(currentTimeMillis, todayStartMillis)
        val completedTaskIds = dao.getCompletedTaskIds()
        val pastEventsDeleted = dao.deletePastEvents(currentTimeMillis, todayStartMillis)
        val completedTasksDeleted = dao.deleteCompletedTasks()
        val recurringOccurrencesDismissed = dao.dismissCompletedTaskOccurrences()
        return CleanupResult(
            completedTasksDeleted,
            recurringOccurrencesDismissed,
            pastEventsDeleted,
            completedTaskIds,
            pastEventIds
        )
    }

    /**
     * Fetches all events and tasks for a given day, merges them into the unified domain model,
     * and returns them in a flow sorted by their display time.
     */
    fun getCalendarItemsForDay(date: LocalDate): Flow<List<CalendarItem>> {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            dao.getEventsOnDay(dayStart, nextDayStart),
            dao.getTasksOnDay(dayStart, nextDayStart),
            dao.getTaskOccurrenceCompletions(dayStart)
        ) { events, tasks, completions ->
        // Map raw entities to the unified domain model
        val eventItems = events.mapNotNull { event ->
            val anchorStart = event.getStartTime()
            val anchorDate = anchorStart.toLocalDate()
            val untilDate = event.repeatUntilMillis?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            }
            if (!occursOnDate(anchorDate, date, event.repeatInterval, event.repeatUnit, untilDate)) {
                return@mapNotNull null
            }
            val occurrenceStart = date.atTime(anchorStart.toLocalTime())
            val duration = Duration.between(anchorStart, event.getEndTime())
            CalendarItem.EventItem(
                id = event.id,
                title = event.title,
                description = event.description,
                startTime = occurrenceStart,
                endTime = occurrenceStart.plus(duration),
                location = event.location,
                reminderMinutesBefore = event.reminderMinutesBefore,
                repeatInterval = event.repeatInterval,
                repeatUnit = event.repeatUnit,
                repeatUntilMillis = event.repeatUntilMillis,
                anchorStartTimeMillis = event.startTimeMillis,
                anchorEndTimeMillis = event.endTimeMillis
            )
        }

        val taskItems = tasks.mapNotNull { task ->
            val anchorDate = Instant.ofEpochMilli(task.dueDateMillis).atZone(zone).toLocalDate()
            val untilDate = task.repeatUntilMillis?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            }
            if (!occursOnDate(anchorDate, date, task.repeatInterval, task.repeatUnit, untilDate)) {
                return@mapNotNull null
            }
            val completion = if (task.repeatInterval > 0) {
                completions.firstOrNull { it.taskId == task.id }
            } else {
                null
            }
            if (completion?.isDismissed == true) return@mapNotNull null
            CalendarItem.TaskItem(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = date,
                isCompleted = if (task.repeatInterval > 0) completion != null else task.isCompleted,
                reminderEnabled = task.reminderEnabled,
                repeatInterval = task.repeatInterval,
                repeatUnit = task.repeatUnit,
                repeatUntilMillis = task.repeatUntilMillis,
                anchorDueDate = anchorDate
            )
        }

        // Merge and sort: The combine function emits when either flow updates.
        // We must sort the merged list by displayTime (chronological).
        (eventItems + taskItems).sortedBy { item -> item.displayTime }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun setTaskOccurrenceCompleted(
        taskId: Int,
        dueDateMillis: Long,
        isCompleted: Boolean
    ) {
        if (isCompleted) {
            dao.setTaskOccurrenceCompleted(TaskOccurrenceCompletion(taskId, dueDateMillis))
        } else {
            dao.clearTaskOccurrenceCompleted(taskId, dueDateMillis)
        }
    }
}