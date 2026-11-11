package com.example.neurocalendar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    // Store the start of the due day as epoch millis for consistency with Room usage.
    val dueDateMillis: Long,
    val isCompleted: Boolean = false
)