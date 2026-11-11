package com.example.neurocalendar.data.dao

import androidx.room.*
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.model.Task
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
    @Query("SELECT * FROM events WHERE startTimeMillis >= :startTimeMillis AND endTimeMillis <= :endTimeMillis ORDER BY startTimeMillis ASC")
    fun getEventsOnDay(startTimeMillis: Long, endTimeMillis: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE reminderMinutesBefore > 0 AND startTimeMillis > :currentTimeMillis")
    suspend fun getUpcomingReminderEvents(currentTimeMillis: Long): List<Event>

    // --- Task Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    /**
     * Fetches tasks for a specific due day (start of the target day as epoch millis).
     */
    @Query("SELECT * FROM tasks WHERE dueDateMillis = :targetDateMillis ORDER BY dueDateMillis ASC")
    fun getTasksOnDay(targetDateMillis: Long): Flow<List<Task>>
}