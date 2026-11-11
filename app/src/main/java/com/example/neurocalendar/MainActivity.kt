package com.example.neurocalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.neurocalendar.data.database.CalendarDatabase
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.repository.CalendarRepository
import com.example.neurocalendar.ui.DayView
import com.example.neurocalendar.ui.EventEditorDialog
import com.example.neurocalendar.ui.theme.NeuroCalendarTheme
import com.example.neurocalendar.viewmodel.DayViewModel
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeuroCalendarTheme {
                // Initialize the root screen content, which is now the DayView
                CalendarRootScreen()
            }
        }
    }
}

/**
 * The main container for the calendar view. This acts as CalendarRootScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRootScreen() {
    val context = LocalContext.current
    val database = remember(context) { CalendarDatabase.getInstance(context) }
    val repository = remember(database) { CalendarRepository(database.calendarDao()) }

    // Use ViewModelProvider to scope the view model lifecycle to the root screen.
    val viewModel: DayViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        DayViewModel(repository, androidx.lifecycle.SavedStateHandle())
    }

    // Collect state from the ViewModel's flow
    val currentItems by viewModel.calendarItems.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showEventEditor by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectDate(selectedDate.minusDays(1)) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = { viewModel.selectDate(selectedDate.plusDays(1)) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
                }
            }
            Button(
                onClick = {
                    eventToEdit = null
                    showEventEditor = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add event")
            }
            DayView(
                items = currentItems,
                date = selectedDate,
                onEditEvent = { item ->
                    val zone = ZoneId.systemDefault()
                    eventToEdit = Event(
                        id = item.id,
                        title = item.title,
                        description = item.description,
                        startTimeMillis = item.startTime.atZone(zone).toInstant().toEpochMilli(),
                        endTimeMillis = item.endTime.atZone(zone).toInstant().toEpochMilli(),
                        location = item.location,
                        reminderMinutesBefore = item.reminderMinutesBefore
                    )
                    showEventEditor = true
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC)
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            viewModel.selectDate(
                                Instant.ofEpochMilli(selectedMillis)
                                    .atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Choose day")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showEventEditor) {
        EventEditorDialog(
            date = selectedDate,
            event = eventToEdit,
            onDismiss = {
                showEventEditor = false
                eventToEdit = null
            },
            onSave = viewModel::saveEvent
        )
    }
}