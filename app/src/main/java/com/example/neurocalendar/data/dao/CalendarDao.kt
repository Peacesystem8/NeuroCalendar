package com.example.neurocalendar.data.dao

import androidx.room.*
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.model.Task
import com.example.neurocalendar.data.model.TaskOccurrenceCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    // --- Event Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    /**
     * Fetches events for a given day range (start of day to end of day + 1 microsecond).
     * Compares against epoch millis for efficient SQL querying.
     */
    @Query("SELECT * FROM events WHERE (startTimeMillis >= :startTimeMillis AND startTimeMillis < :endTimeMillis) OR (repeatInterval > 0 AND startTimeMillis < :endTimeMillis AND (repeatUntilMillis IS NULL OR repeatUntilMillis >= :startTimeMillis)) ORDER BY startTimeMillis ASC")
    fun getEventsOnDay(startTimeMillis: Long, endTimeMillis: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int): Event?

    @Query("SELECT * FROM events WHERE reminderMinutesBefore > 0 AND (startTimeMillis > :currentTimeMillis OR (repeatInterval > 0 AND (repeatUntilMillis IS NULL OR repeatUntilMillis >= :todayStartMillis)))")
    suspend fun getUpcomingReminderEvents(
        currentTimeMillis: Long,
        todayStartMillis: Long
    ): List<Event>

    @Query("SELECT id FROM events WHERE (repeatInterval = 0 AND endTimeMillis <= :currentTimeMillis) OR (repeatInterval > 0 AND repeatUntilMillis IS NOT NULL AND repeatUntilMillis < :todayStartMillis)")
    suspend fun getPastEventIds(currentTimeMillis: Long, todayStartMillis: Long): List<Int>

    @Query("DELETE FROM events WHERE (repeatInterval = 0 AND endTimeMillis <= :currentTimeMillis) OR (repeatInterval > 0 AND repeatUntilMillis IS NOT NULL AND repeatUntilMillis < :todayStartMillis)")
    suspend fun deletePastEvents(currentTimeMillis: Long, todayStartMillis: Long): Int

    // --- Task Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks(): Int

    @Query("SELECT id FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskIds(): List<Int>

    @Query("UPDATE task_occurrence_completions SET isDismissed = 1 WHERE isDismissed = 0")
    suspend fun dismissCompletedTaskOccurrences(): Int

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND isCompleted = 0 AND ((repeatInterval = 0 AND dueDateMillis >= :todayStartMillis) OR (repeatInterval > 0 AND (repeatUntilMillis IS NULL OR repeatUntilMillis >= :todayStartMillis)))")
    suspend fun getUpcomingTaskReminders(todayStartMillis: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM task_occurrence_completions WHERE dueDateMillis = :dueDateMillis")
    fun getTaskOccurrenceCompletions(dueDateMillis: Long): Flow<List<TaskOccurrenceCompletion>>

    @Query("SELECT * FROM task_occurrence_completions WHERE taskId = :taskId AND dueDateMillis = :dueDateMillis")
    suspend fun getTaskOccurrenceCompletion(
        taskId: Int,
        dueDateMillis: Long
    ): TaskOccurrenceCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTaskOccurrenceCompleted(completion: TaskOccurrenceCompletion)

    @Query("DELETE FROM task_occurrence_completions WHERE taskId = :taskId AND dueDateMillis = :dueDateMillis")
    suspend fun clearTaskOccurrenceCompleted(taskId: Int, dueDateMillis: Long)

    /**
     * Fetches tasks for a specific due day (start of the target day as epoch millis).
     */
    @Query("SELECT * FROM tasks WHERE (dueDateMillis >= :targetDateMillis AND dueDateMillis < :nextDayMillis) OR (repeatInterval > 0 AND dueDateMillis < :nextDayMillis AND (repeatUntilMillis IS NULL OR repeatUntilMillis >= :targetDateMillis)) ORDER BY dueDateMillis ASC")
    fun getTasksOnDay(targetDateMillis: Long, nextDayMillis: Long): Flow<List<Task>>
}