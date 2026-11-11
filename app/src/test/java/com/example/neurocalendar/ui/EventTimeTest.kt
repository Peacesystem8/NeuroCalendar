package com.example.neurocalendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class EventTimeTest {
    @Test
    fun parses24HourTimesWithOneOrTwoDigitHours() {
        assertEquals(LocalTime.of(9, 30), parseEventTime("9:30"))
        assertEquals(LocalTime.of(23, 59), parseEventTime("23:59"))
    }

    @Test
    fun rejectsMalformedOrOutOfRangeTimes() {
        assertNull(parseEventTime("24:00"))
        assertNull(parseEventTime("12:60"))
        assertNull(parseEventTime("noon"))
    }
}
