package com.fliptimer.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The timer's value at the end (midnight) of [date]. */
data class DayEnd(val date: LocalDate, val valueMillis: Long)

fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun startOfDayMillis(date: LocalDate, zone: ZoneId): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

fun nextMidnight(now: Long, zone: ZoneId): Long =
    startOfDayMillis(localDate(now, zone).plusDays(1), zone)

/**
 * Day-end values not yet recorded: every day after [lastLogged] up to yesterday.
 *
 * This is exact as long as it runs before every timer change: the timer's value
 * follows one straight line between changes, so [state] still knows what the value
 * was at each midnight since the last change.
 */
fun pendingDayEnds(
    state: TimerState,
    lastLogged: LocalDate?,
    today: LocalDate,
    zone: ZoneId,
    maxDays: Long = 400,
): List<DayEnd> {
    if (lastLogged == null) return emptyList()
    val first = maxOf(lastLogged.plusDays(1), today.minusDays(maxDays))
    val result = mutableListOf<DayEnd>()
    var d = first
    while (d < today) {
        result += DayEnd(d, state.valueAt(startOfDayMillis(d.plusDays(1), zone)))
        d = d.plusDays(1)
    }
    return result
}

/** How many of the most recent [days] entries ended at 0:00 or above. */
fun daysEndedNonNegative(log: List<DayEnd>, days: Int): Int =
    log.sortedByDescending { it.date }.take(days).count { displaySeconds(it.valueMillis) >= 0 }
