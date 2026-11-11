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
    fun parsesAmPmTimesIncludingNoonAndMidnight() {
        assertEquals(LocalTime.of(9, 30), parseEventTime("9:30 AM"))
        assertEquals(LocalTime.of(21, 30), parseEventTime("9:30 PM"))
        assertEquals(LocalTime.MIDNIGHT, parseEventTime("12:00 AM"))
        assertEquals(LocalTime.NOON, parseEventTime("12:00 PM"))
        assertEquals(LocalTime.of(21, 30), parseEventTime("9:30pm"))
    }

    @Test
    fun rejectsMalformedOrOutOfRangeTimes() {
        assertNull(parseEventTime("24:00"))
        assertNull(parseEventTime("12:60"))
        assertNull(parseEventTime("13:00 PM"))
        assertNull(parseEventTime("noon"))
    }
}
