package com.example.neurocalendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.neurocalendar.domain.model.CalendarItem
import com.example.neurocalendar.ui.theme.NeuroCalendarTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Displays the daily schedule in a clean, chronological, and accessible manner.
 * This composable is designed to be the primary content of the calendar screen.
 *
 * @param items The list of CalendarItems fetched for the selected day, guaranteed to be sorted chronologically.
 */
@Composable
fun DayView(
    items: List<CalendarItem>,
    date: LocalDate,
    onEditEvent: (CalendarItem.EventItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DateHeader(date)
        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            DayEmptyView(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(items) { item ->
                    DayItemCard(item = item, onEditEvent = onEditEvent)
                }
            }
        }
    }
}

/**
 * Composable that displays the selected date header.
 */
@Composable
fun DateHeader(date: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    val formattedDateString = date.format(formatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Date Icon",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = formattedDateString,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Daily Schedule",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Empty state composable when no events or tasks are found for the selected day.
 */
@Composable
fun DayEmptyView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CozyCalendarIllustration()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A little breathing room",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "No plans on the calendar for this day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Take a slow breath, or add something lovely.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CozyCalendarIllustration() {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.size(width = 184.dp, height = 148.dp)) {
        val scale = size.width / 184f
        fun point(value: Float) = value * scale

        drawCircle(
            color = colors.tertiaryContainer,
            radius = point(57f),
            center = Offset(point(111f), point(70f))
        )
        drawRoundRect(
            color = colors.surface,
            topLeft = Offset(point(41f), point(27f)),
            size = Size(point(96f), point(90f)),
            cornerRadius = CornerRadius(point(17f))
        )
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(point(41f), point(27f)),
            size = Size(point(96f), point(34f)),
            cornerRadius = CornerRadius(point(17f))
        )
        drawRect(
            color = colors.primary,
            topLeft = Offset(point(41f), point(44f)),
            size = Size(point(96f), point(17f))
        )
        listOf(68f, 110f).forEach { x ->
            drawLine(
                color = colors.onPrimary,
                start = Offset(point(x), point(20f)),
                end = Offset(point(x), point(39f)),
                strokeWidth = point(4f),
                cap = StrokeCap.Round
            )
        }
        val dateDots = listOf(
            Triple(64f, 76f, colors.tertiary),
            Triple(89f, 76f, colors.secondary),
            Triple(114f, 76f, colors.secondary),
            Triple(64f, 98f, colors.secondary),
            Triple(89f, 98f, colors.tertiary),
            Triple(114f, 98f, colors.secondary)
        )
        dateDots.forEach { (x, y, color) ->
            drawCircle(color, point(4f), Offset(point(x), point(y)))
        }
        drawLine(
            color = colors.secondary,
            start = Offset(point(137f), point(119f)),
            end = Offset(point(153f), point(91f)),
            strokeWidth = point(3f),
            cap = StrokeCap.Round
        )
        drawOval(
            color = colors.secondaryContainer,
            topLeft = Offset(point(135f), point(96f)),
            size = Size(point(15f), point(8f))
        )
        drawOval(
            color = colors.secondaryContainer,
            topLeft = Offset(point(148f), point(85f)),
            size = Size(point(15f), point(8f))
        )
    }
}

/**
 * Displays a single calendar item (Event or Task).
 */
@Composable
fun DayItemCard(
    item: CalendarItem,
    onEditEvent: (CalendarItem.EventItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use an animated card for expressive motion
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Section for Title and Type Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isEvent = item is CalendarItem.EventItem
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isEvent) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = if (isEvent) "EVENT" else "TASK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEvent) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (item is CalendarItem.EventItem) {
                    TextButton(onClick = { onEditEvent(item) }) {
                        Text("Edit event")
                    }
                }
            }

            // Main Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time/Date Display (Most critical visual element)
            val timeDisplay = when (item) {
                is CalendarItem.EventItem ->
                    "${item.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${item.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                is CalendarItem.TaskItem ->
                    "Due: ${item.dueDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            }

            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Details/Description (Ample white space focus)
            Text(
                text = item.details ?: "No details provided.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item is CalendarItem.EventItem && item.location != null) {
                 HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                 Text(text = "📍 ${item.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDayView() {
    NeuroCalendarTheme {
        // Mock data for preview
        val mockItems = listOf<CalendarItem>(
            CalendarItem.EventItem(
                id = 1, title = "Team Sync-Up", description = "Review Q3 roadmap.", startTime = LocalDateTime.of(2024, 7, 15, 10, 0), endTime = LocalDateTime.of(2024, 7, 15, 11, 0), location = "Zoom Call"
            ),
            CalendarItem.TaskItem(
                id = 2, title = "Draft Project Proposal", description = "Finalize budget and scope.", dueDate = LocalDate.of(2024, 7, 15), isCompleted = false
            ),
            CalendarItem.EventItem(
                id = 3, title = "Client Demo Call", description = "Showcase new features to Alpha Corp.", startTime = LocalDateTime.of(2024, 7, 15, 14, 30), endTime = LocalDateTime.of(2024, 7, 15, 15, 30), location = "Office B"
            )
        ).sortedBy { it.displayTime }

        DayView(
            items = mockItems,
            date = LocalDate.of(2024, 7, 15),
            onEditEvent = {}
        )
    }
}