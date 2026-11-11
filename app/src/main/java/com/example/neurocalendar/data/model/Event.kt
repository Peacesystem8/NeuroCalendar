package com.example.neurocalendar.data.model

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    // Room requires primitives/Long for time storage (epoch millis).
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val location: String? = null,
    @ColumnInfo(defaultValue = "0") val reminderMinutesBefore: Int = 0,
    @ColumnInfo(defaultValue = "0") val repeatInterval: Int = 0,
    @ColumnInfo(defaultValue = "'WEEK'") val repeatUnit: String = "WEEK",
    val repeatUntilMillis: Long? = null
) {
    /** Converts stored epoch millis to the device's local time. */
    fun getStartTime(): LocalDateTime =
        Instant.ofEpochMilli(startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun getEndTime(): LocalDateTime =
        Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
}