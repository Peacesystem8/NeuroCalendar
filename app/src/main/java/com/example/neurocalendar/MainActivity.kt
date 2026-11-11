package com.example.neurocalendar

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Check
import com.example.neurocalendar.data.database.CalendarDatabase
import com.example.neurocalendar.data.model.Event
import com.example.neurocalendar.data.model.Task
import com.example.neurocalendar.R
import com.example.neurocalendar.data.repository.CalendarRepository
import com.example.neurocalendar.notifications.EventReminderScheduler
import com.example.neurocalendar.notifications.TaskReminderScheduler
import com.example.neurocalendar.ui.DayView
import com.example.neurocalendar.ui.EventEditorDialog
import com.example.neurocalendar.ui.TaskEditorDialog
import com.example.neurocalendar.ui.theme.NeuroCalendarTheme
import com.example.neurocalendar.viewmodel.DayViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import java.time.ZoneOffset
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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
    val reminderScheduler = remember(context) { EventReminderScheduler(context) }
    val taskReminderScheduler = remember(context) { TaskReminderScheduler(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingReminderEvent by remember { mutableStateOf<Event?>(null) }
    var pendingReminderTask by remember { mutableStateOf<Task?>(null) }
    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val event = pendingReminderEvent
        val task = pendingReminderTask
        pendingReminderEvent = null
        pendingReminderTask = null
        if (event != null) {
            if (EventReminderScheduler.notificationsAllowed(context) &&
                reminderScheduler.canScheduleExactAlarms()
            ) {
                reminderScheduler.schedule(event)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Allow Alarms & reminders access in Settings to receive punctual reminders."
                    )
                }
            }
        }
        if (task != null) {
            if (EventReminderScheduler.notificationsAllowed(context) &&
                taskReminderScheduler.canScheduleExactAlarms()
            ) {
                taskReminderScheduler.schedule(task)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Allow Alarms & reminders access to receive task reminders."
                    )
                }
            }
        }
    }
    fun scheduleOrRequestExactAlarm(event: Event) {
        if (reminderScheduler.requiresExactAlarmAccess(event)) {
            pendingReminderEvent = event
            exactAlarmPermissionLauncher.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } else {
            reminderScheduler.schedule(event)
        }
    }
    fun scheduleOrRequestExactTaskAlarm(task: Task) {
        if (taskReminderScheduler.requiresExactAlarmAccess(task)) {
            pendingReminderTask = task
            exactAlarmPermissionLauncher.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        } else {
            taskReminderScheduler.schedule(task)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val event = pendingReminderEvent
        val task = pendingReminderTask
        pendingReminderEvent = null
        pendingReminderTask = null
        if (event != null && granted && EventReminderScheduler.notificationsAllowed(context)) {
            scheduleOrRequestExactAlarm(event)
        } else if (event != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Event saved. Turn on notifications in Settings to receive reminders."
                )
            }
        }
        if (task != null && granted && EventReminderScheduler.notificationsAllowed(context)) {
            scheduleOrRequestExactTaskAlarm(task)
        } else if (task != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Task saved. Turn on notifications in Settings to receive reminders."
                )
            }
        }
    }
    fun scheduleTaskReminder(task: Task) {
        if (!task.reminderEnabled || task.isCompleted) {
            taskReminderScheduler.cancel(task.id)
        } else if (EventReminderScheduler.notificationsAllowed(context)) {
            scheduleOrRequestExactTaskAlarm(task)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminderTask = task
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Task saved. Turn on notifications in Settings to receive reminders."
                )
            }
        }
    }

    // Use ViewModelProvider to scope the view model lifecycle to the root screen.
    val viewModel: DayViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        DayViewModel(repository, androidx.lifecycle.SavedStateHandle())
    }

    // Collect state from the ViewModel's flow
    val currentItems by viewModel.calendarItems.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showEventEditor by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    var showTaskEditor by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCleanupConfirmation by remember { mutableStateOf(false) }

    suspend fun rescheduleUpcomingReminders() {
        if (EventReminderScheduler.notificationsAllowed(context)) {
            viewModel.getUpcomingReminderEvents(System.currentTimeMillis())
                .forEach(reminderScheduler::schedule)
            val zone = ZoneId.systemDefault()
            val todayStartMillis = java.time.LocalDate.now(zone)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
            viewModel.getUpcomingTaskReminders(todayStartMillis)
                .forEach(taskReminderScheduler::schedule)
        }
    }

    LaunchedEffect(viewModel) {
        rescheduleUpcomingReminders()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { rescheduleUpcomingReminders() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.aff_girl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.58f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Moonlit plans",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "A softer place for your day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                    ),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.selectDate(selectedDate.minusDays(1)) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(
                                text = selectedDate.format(
                                    DateTimeFormatter.ofPattern("EEEE, MMMM d")
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.selectDate(selectedDate.plusDays(1)) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            eventToEdit = null
                            showEventEditor = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add event")
                    }
                    OutlinedButton(
                        onClick = {
                            taskToEdit = null
                            showTaskEditor = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add task")
                    }
                }
                TextButton(
                    onClick = { showCleanupConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear completed tasks & past events")
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
                            startTimeMillis = item.anchorStartTimeMillis
                                ?: item.startTime.atZone(zone).toInstant().toEpochMilli(),
                            endTimeMillis = item.anchorEndTimeMillis
                                ?: item.endTime.atZone(zone).toInstant().toEpochMilli(),
                            location = item.location,
                            reminderMinutesBefore = item.reminderMinutesBefore,
                            repeatInterval = item.repeatInterval,
                            repeatUnit = item.repeatUnit,
                            repeatUntilMillis = item.repeatUntilMillis
                        )
                        showEventEditor = true
                    },
                    onEditTask = { item ->
                        taskToEdit = Task(
                            id = item.id,
                            title = item.title,
                            description = item.description,
                            dueDateMillis = item.anchorDueDate
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli(),
                            isCompleted = if (item.repeatInterval > 0) {
                                false
                            } else {
                                item.isCompleted
                            },
                            reminderEnabled = item.reminderEnabled,
                            repeatInterval = item.repeatInterval,
                            repeatUnit = item.repeatUnit,
                            repeatUntilMillis = item.repeatUntilMillis
                        )
                        showTaskEditor = true
                    },
                    onTaskCompletionChange = { item, isCompleted ->
                        scope.launch {
                            try {
                                val occurrenceMillis = item.dueDate
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant().toEpochMilli()
                                val task = Task(
                                    id = item.id,
                                    title = item.title,
                                    description = item.description,
                                    dueDateMillis = item.anchorDueDate
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli(),
                                    isCompleted = if (item.repeatInterval > 0) {
                                        false
                                    } else {
                                        isCompleted
                                    },
                                    reminderEnabled = item.reminderEnabled,
                                    repeatInterval = item.repeatInterval,
                                    repeatUnit = item.repeatUnit,
                                    repeatUntilMillis = item.repeatUntilMillis
                                )
                                if (item.repeatInterval > 0) {
                                    viewModel.setTaskOccurrenceCompleted(
                                        item.id,
                                        occurrenceMillis,
                                        isCompleted
                                    )
                                    scheduleTaskReminder(task)
                                } else {
                                    scheduleTaskReminder(viewModel.saveTask(task))
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Log.e("CalendarRootScreen", "Unable to update task completion", error)
                                snackbarHostState.showSnackbar(
                                    "Couldn't update the task. Please try again."
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
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
            date = eventToEdit?.getStartTime()?.toLocalDate() ?: selectedDate,
            event = eventToEdit,
            onDismiss = {
                showEventEditor = false
                eventToEdit = null
            },
            onSave = { event ->
                val savedEvent = viewModel.saveEvent(event)
                val notificationsAllowed =
                    EventReminderScheduler.notificationsAllowed(context)

                if (savedEvent.reminderMinutesBefore == 0 || !notificationsAllowed) {
                    reminderScheduler.cancel(savedEvent.id)
                }
                if (savedEvent.reminderMinutesBefore > 0) {
                    if (notificationsAllowed) {
                        scheduleOrRequestExactAlarm(savedEvent)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingReminderEvent = savedEvent
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Event saved. Turn on notifications in Settings to receive reminders."
                            )
                        }
                    }
                }
            }
        )
    }

    if (showTaskEditor) {
        TaskEditorDialog(
            date = taskToEdit?.let {
                Instant.ofEpochMilli(it.dueDateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } ?: selectedDate,
            task = taskToEdit,
            onDismiss = {
                showTaskEditor = false
                taskToEdit = null
            },
            onSave = { task ->
                val savedTask = viewModel.saveTask(task)
                scheduleTaskReminder(savedTask)
            }
        )
    }

    if (showCleanupConfirmation) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirmation = false },
            title = { Text("Clear finished items?") },
            text = {
                Text(
                    "This permanently deletes completed tasks and events that have ended, " +
                        "across all dates. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanupConfirmation = false
                        scope.launch {
                            try {
                                val result = viewModel.clearCompletedTasksAndPastEvents(
                                    System.currentTimeMillis()
                                )
                                result.pastEventIds.forEach(reminderScheduler::cancel)
                                result.completedTaskIds.forEach(taskReminderScheduler::cancel)
                                val message = if (
                                    result.completedTasksDeleted == 0 &&
                                    result.recurringOccurrencesDismissed == 0 &&
                                    result.pastEventsDeleted == 0
                                ) {
                                    "Nothing to clear."
                                } else {
                                    "Removed ${result.completedTasksDeleted} completed tasks, " +
                                        "${result.recurringOccurrencesDismissed} completed repeats, and " +
                                        "${result.pastEventsDeleted} past events."
                                }
                                snackbarHostState.showSnackbar(message)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Log.e("CalendarRootScreen", "Unable to clear finished items", error)
                                snackbarHostState.showSnackbar(
                                    "Couldn't clear finished items. Please try again."
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear items")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}