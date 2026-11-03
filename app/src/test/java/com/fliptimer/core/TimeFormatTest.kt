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

    @Test fun durations() {
        assertEquals("45s", formatDuration(45_000))
        assertEquals("12m 30s", formatDuration(750_000))
        assertEquals("5h 12m", formatDuration((5 * 3600 + 12 * 60) * 1000L))
        assertEquals("2d 4h", formatDuration((2 * 86400 + 4 * 3600 + 59) * 1000L))
        assertEquals("0s", formatDuration(-5_000))
    }

    @Test fun countdown() {
        assertEquals("1h 0m", formatCountdown(3_600_000))
        assertEquals("overdue 3m 0s", formatCountdown(-180_000))
    }
}
