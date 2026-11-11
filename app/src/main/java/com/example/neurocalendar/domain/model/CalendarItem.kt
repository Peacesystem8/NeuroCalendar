package com.example.neurocalendar.domain.model

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unified domain model representing any time-based item on the calendar.
 */
sealed class CalendarItem {
    abstract val id: Int
    abstract val title: String
    abstract val details: String?
    abstract val displayTime: LocalDateTime // The primary reference point for sorting and display

    data class EventItem(
        override val id: Int,
        override val title: String,
        val description: String?,
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val location: String?,
        val reminderMinutesBefore: Int = 0,
        override val details: String? = description
    ) : CalendarItem() {
        override val displayTime: LocalDateTime = startTime

        companion object {
            fun calculateDisplayTime(start: LocalDateTime, end: LocalDateTime): LocalDateTime = start
        }
    }

    data class TaskItem(
        override val id: Int,
        override val title: String,
        val description: String?,
        val dueDate: LocalDate,
        val isCompleted: Boolean,
        override val details: String? = description
    ) : CalendarItem() {
        // For tasks, the primary time reference is the start of the day they are due (at midnight).
        override val displayTime: LocalDateTime = LocalDateTime.of(dueDate, LocalTime.MIDNIGHT)

        companion object {}
    }
}