package com.example.neurocalendar.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurocalendar.data.repository.CalendarRepository
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.model.Task
import com.example.neurocalendar.domain.model.CalendarItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel responsible for managing and providing the current day's calendar items.
 */
class DayViewModel(private val repository: CalendarRepository, savedStateHandle: SavedStateHandle) : ViewModel() {

    // Key to retrieve the selected date from navigation arguments/saved state
    companion object {
        const val SELECTED_DATE_KEY = "selectedDate"
    }

    /**
     * StateFlow that emits the list of calendar items for the current day.
     */
    private val _calendarItems: MutableStateFlow<List<CalendarItem>> = MutableStateFlow(emptyList())
    val calendarItems: StateFlow<List<CalendarItem>> = _calendarItems

    /**
     * Tracks the currently selected date, derived from saved state or defaulted to today.
     */
    private val _selectedDate: MutableStateFlow<LocalDate> = MutableStateFlow(
        savedStateHandle.get<String>(SELECTED_DATE_KEY)?.let { LocalDate.parse(it) } ?: LocalDate.now()
    )
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    init {
        // Start observing the repository whenever the date changes.
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                repository.getCalendarItemsForDay(date).collect { items ->
                    _calendarItems.value = items
                }
            }
        }
    }

    /**
     * Public method to change the selected day and trigger a data refresh.
     */
    fun selectDate(newDate: LocalDate) {
        if (newDate != _selectedDate.value) {
            _selectedDate.value = newDate
        }
    }

    suspend fun saveEvent(event: Event): Event = repository.saveEvent(event)

    suspend fun saveTask(task: Task): Task = repository.saveTask(task)

    suspend fun getUpcomingTaskReminders(todayStartMillis: Long): List<Task> =
        repository.getUpcomingTaskReminders(todayStartMillis)

    suspend fun setTaskOccurrenceCompleted(
        taskId: Int,
        dueDateMillis: Long,
        isCompleted: Boolean
    ) = repository.setTaskOccurrenceCompleted(taskId, dueDateMillis, isCompleted)

    suspend fun clearCompletedTasksAndPastEvents(currentTimeMillis: Long) =
        repository.clearCompletedTasksAndPastEvents(currentTimeMillis)

    suspend fun getUpcomingReminderEvents(currentTimeMillis: Long): List<Event> =
        repository.getUpcomingReminderEvents(currentTimeMillis)

    // Helper function for UI/Testing: get current state date
    fun getCurrentDate(): LocalDate = _selectedDate.value
}