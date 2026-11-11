package com.example.neurocalendar.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.neurocalendar.data.model.Event
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventEditorDialog(
    date: LocalDate,
    event: Event?,
    onDismiss: () -> Unit,
    onSave: suspend (Event) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val eventStart = remember(event?.id) {
        event?.let { Instant.ofEpochMilli(it.startTimeMillis).atZone(zone).toLocalTime() }
    }
    val eventEnd = remember(event?.id) {
        event?.let { Instant.ofEpochMilli(it.endTimeMillis).atZone(zone).toLocalTime() }
    }
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var startText by remember(event?.id) {
        mutableStateOf(eventStart?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "09:00")
    }
    var endText by remember(event?.id) {
        mutableStateOf(eventEnd?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "10:00")
    }
    var notes by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var showOptionalFields by remember(event?.id) {
        mutableStateOf(!event?.description.isNullOrBlank() || !event?.location.isNullOrBlank())
    }
    var attemptedSave by remember(event?.id) { mutableStateOf(false) }
    var isSaving by remember(event?.id) { mutableStateOf(false) }
    var saveError by remember(event?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val startTime = parseEventTime(startText)
    val endTime = parseEventTime(endText)
    val invalidTimeOrder = startTime != null && endTime != null && endTime <= startTime

    fun saveEvent() {
        attemptedSave = true
        if (title.isBlank() || startTime == null || endTime == null || invalidTimeOrder) return

        val savedEvent = Event(
            id = event?.id ?: 0,
            title = title.trim(),
            description = notes.trim().ifBlank { null },
            startTimeMillis = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = date.atTime(endTime).atZone(zone).toInstant().toEpochMilli(),
            location = location.trim().ifBlank { null }
        )

        scope.launch {
            isSaving = true
            saveError = null
            try {
                onSave(savedEvent)
                onDismiss()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("EventEditorDialog", "Unable to save event", error)
                saveError = "Couldn't save the event. Please try again."
            } finally {
                isSaving = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (event == null) "Add event" else "Edit event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "For ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}. " +
                        "A name and time are all you need.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Event name") },
                    singleLine = true,
                    isError = attemptedSave && title.isBlank(),
                    supportingText = {
                        if (attemptedSave && title.isBlank()) Text("Enter an event name.")
                    }
                )
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Start time") },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = attemptedSave && startTime == null,
                    supportingText = {
                        if (attemptedSave && startTime == null) {
                            Text("Use 24-hour time, for example 09:30.")
                        }
                    }
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End time") },
                    placeholder = { Text("10:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = attemptedSave && (endTime == null || invalidTimeOrder),
                    supportingText = {
                        if (attemptedSave && endTime == null) {
                            Text("Use 24-hour time, for example 10:30.")
                        } else if (attemptedSave && invalidTimeOrder) {
                            Text("Choose an end time after the start time.")
                        }
                    }
                )
                TextButton(onClick = { showOptionalFields = !showOptionalFields }) {
                    Text(if (showOptionalFields) "Hide optional details" else "Add notes or location")
                }
                if (showOptionalFields) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes (optional)") },
                        minLines = 2,
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location (optional)") },
                        singleLine = true
                    )
                }
                if (saveError != null) {
                    Text(
                        text = saveError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(onClick = ::saveEvent, enabled = !isSaving) {
                Text(if (isSaving) "Saving…" else "Save event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

internal fun parseEventTime(value: String): LocalTime? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute)
}
