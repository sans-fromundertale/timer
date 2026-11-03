package com.fliptimer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun clock() {
        assertEquals("5:00", formatMillis(300_000))
        assertEquals("0:01", formatMillis(1))
        assertEquals("0:00", formatMillis(0))
        assertEquals("0:00", formatMillis(-999))
        assertEquals("−0:01", formatMillis(-1_000))
        assertEquals("1:02:03", formatSeconds(3723))
        assertEquals("−1:02:03", formatSeconds(-3723))
    }

    @Test fun signed() {
        assertEquals("+1:00", formatSignedMillis(60_000))
        assertEquals("−1:00", formatSignedMillis(-60_000))
        assertEquals("0:00", formatSignedMillis(0))
    }

    @Test fun deadlineClock() {
        assertEquals("0:00:00", formatDeadlineClock(0))
        assertEquals("0:00:01", formatDeadlineClock(1))
        assertEquals("4:13:22", formatDeadlineClock((4 * 3600 + 13 * 60 + 22) * 1000L))
        assertEquals("2d 04:13:22", formatDeadlineClock(((2 * 86400) + 4 * 3600 + 13 * 60 + 22) * 1000L))
        assertEquals("−0:05:00", formatDeadlineClock(-300_000))
        assertEquals("0:00:00", formatDeadlineClock(-999))
    }
}
