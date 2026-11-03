package com.fliptimer.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayLogTest {
    private val zone = ZoneId.of("America/Toronto")
    private val nov1 = LocalDate.of(2026, 11, 1) // DST ends Nov 1 2026: a 25-hour day
    private val hour = 3_600_000L

    @Test fun firstRunRecordsNothing() {
        assertEquals(0, pendingDayEnds(TimerState(), null, nov1, zone).size)
    }

    @Test fun recordsValueAtEachMissedMidnight() {
        // Started counting up from 0 at 10pm on Oct 31
        val start = startOfDayMillis(LocalDate.of(2026, 10, 31), zone) + 22 * hour
        val s = TimerState(direction = TimerState.UP).setInitial(0).start(start)
        val today = LocalDate.of(2026, 11, 3)
        val ends = pendingDayEnds(s, LocalDate.of(2026, 10, 30), today, zone)
        assertEquals(listOf(LocalDate.of(2026, 10, 31), nov1, LocalDate.of(2026, 11, 2)), ends.map { it.date })
        assertEquals(2 * hour, ends[0].valueMillis)
        assertEquals(2 * hour + 25 * hour, ends[1].valueMillis) // DST day is 25h long
        assertEquals(2 * hour + 49 * hour, ends[2].valueMillis)
    }

    @Test fun nothingPendingWhenUpToDate() {
        assertEquals(0, pendingDayEnds(TimerState(), nov1.minusDays(1), nov1, zone).size)
    }

    @Test fun capsLongGaps() {
        val ends = pendingDayEnds(TimerState(), nov1.minusDays(1000), nov1, zone, maxDays = 10)
        assertEquals(10, ends.size)
        assertEquals(nov1.minusDays(1), ends.last().date)
    }

    @Test fun nextMidnightAcrossDst() {
        val oct31noon = startOfDayMillis(LocalDate.of(2026, 10, 31), zone) + 12 * hour
        assertEquals(startOfDayMillis(nov1, zone), nextMidnight(oct31noon, zone))
    }

    @Test fun nonNegativeCount() {
        val log = listOf(
            DayEnd(nov1, 5_000), DayEnd(nov1.minusDays(1), -5_000),
            DayEnd(nov1.minusDays(2), -500), DayEnd(nov1.minusDays(3), -9_000),
        )
        assertEquals(2, daysEndedNonNegative(log, 3)) // -500ms displays as 0:00
    }
}
