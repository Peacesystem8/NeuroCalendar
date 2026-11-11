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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        mutableStateOf(eventStart?.format(eventTimeInputFormatter) ?: "9:00 AM")
    }
    var endText by remember(event?.id) {
        mutableStateOf(eventEnd?.format(eventTimeInputFormatter) ?: "10:00 AM")
    }
    var notes by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var reminderMinutesBefore by remember(event?.id) {
        mutableStateOf(event?.reminderMinutesBefore ?: 0)
    }
    var repeatInterval by remember(event?.id) {
        mutableStateOf(event?.repeatInterval ?: 0)
    }
    var repeatUnit by remember(event?.id) {
        mutableStateOf(event?.repeatUnit ?: "WEEK")
    }
    var repeatUntilMillis by remember(event?.id) {
        mutableStateOf(event?.repeatUntilMillis)
    }
    var repeatIntervalValid by remember(event?.id) { mutableStateOf(true) }
    var showReminderOptions by remember(event?.id) { mutableStateOf(false) }
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
    val repeatUntilDate = repeatUntilMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
    }
    val invalidRepeatEnd = repeatInterval > 0 && repeatUntilDate?.isBefore(date) == true
    val validRepeatRule = repeatIntervalValid && (repeatInterval == 0 || !invalidRepeatEnd)

    fun saveEvent() {
        attemptedSave = true
        if (title.isBlank() || startTime == null || endTime == null ||
            invalidTimeOrder || !validRepeatRule
        ) return

        val savedEvent = Event(
            id = event?.id ?: 0,
            title = title.trim(),
            description = notes.trim().ifBlank { null },
            startTimeMillis = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = date.atTime(endTime).atZone(zone).toInstant().toEpochMilli(),
            location = location.trim().ifBlank { null },
            reminderMinutesBefore = reminderMinutesBefore,
            repeatInterval = repeatInterval,
            repeatUnit = repeatUnit,
            repeatUntilMillis = repeatUntilMillis.takeIf { repeatInterval > 0 }
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
                    placeholder = { Text("9:00 AM") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = attemptedSave && startTime == null,
                    supportingText = {
                        if (attemptedSave && startTime == null) {
                            Text("Use 9:30 AM/PM, or 24-hour time such as 21:30.")
                        }
                    }
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End time") },
                    placeholder = { Text("10:00 AM") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = attemptedSave && (endTime == null || invalidTimeOrder),
                    supportingText = {
                        if (attemptedSave && endTime == null) {
                            Text("Use 10:30 AM/PM, or 24-hour time such as 22:30.")
                        } else if (attemptedSave && invalidTimeOrder) {
                            Text("Choose an end time after the start time.")
                        }
                    }
                )
                RecurrenceControls(
                    repeatInterval = repeatInterval,
                    repeatUnit = repeatUnit,
                    repeatUntilMillis = repeatUntilMillis,
                    onRuleChange = { interval, unit, until ->
                        repeatInterval = interval
                        repeatUnit = unit
                        repeatUntilMillis = until
                    },
                    onIntervalValidityChange = { repeatIntervalValid = it }
                )
                if (event != null && repeatInterval > 0) {
                    Text(
                        "Changes apply to the entire event series.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (invalidRepeatEnd) {
                    Text(
                        "The repeat end date must be on or after the first event date.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Reminder", style = MaterialTheme.typography.titleSmall)
                Text(
                    "One quiet notification. No repeats.",
                    style = MaterialTheme.typography.bodySmall
                )
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { showReminderOptions = true }) {
                        Text(reminderLabel(reminderMinutesBefore))
                    }
                    DropdownMenu(
                        expanded = showReminderOptions,
                        onDismissRequest = { showReminderOptions = false }
                    ) {
                        listOf(0, 5, 10, 15, 30).forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(reminderLabel(minutes)) },
                                onClick = {
                                    reminderMinutesBefore = minutes
                                    showReminderOptions = false
                                }
                            )
                        }
                    }
                }
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
            Button(onClick = ::saveEvent, enabled = !isSaving && validRepeatRule) {
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
    val input = value.trim()
    val normalizedInput = Regex("(?i)(?<=\\d)\\s*(AM|PM)$")
        .replace(input) { " ${it.groupValues[1]}" }
    if (normalizedInput.endsWith("AM", ignoreCase = true) ||
        normalizedInput.endsWith("PM", ignoreCase = true)
    ) {
        return try {
            LocalTime.parse(normalizedInput, eventTimeInputFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    val parts = input.split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute)
}

private val eventTimeInputFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("h:mm a")
    .toFormatter(Locale.US)

private fun reminderLabel(minutes: Int): String =
    if (minutes == 0) "No reminder" else "$minutes minutes before"
