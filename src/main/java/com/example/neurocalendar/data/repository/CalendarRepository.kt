// No changes needed yet for the plan creation

/**
 * Data class representing a calendar item.
 */
data class CalendarItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startInstant: Instant,
    val endInstant: Instant,
    val isRecurring: Boolean = false,
    // Add specific fields for Event vs Task here later
)

/**
 * Data class representing a simple to-do task item.
 */
data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDate: Instant,
    var isCompleted: Boolean = false
)

interface CalendarRepository {
    // Existing functions...

    /**
     * Persists a newly created calendar event.
     */
    suspend fun saveEvent(event: Event): Result<Unit>

    /**
     * Persists a new checklist task item.
     */
    suspend fun saveTask(task: TaskItem): Result<Unit>
}