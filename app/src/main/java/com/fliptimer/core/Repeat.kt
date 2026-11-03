package com.fliptimer.core

import java.time.Instant
import java.time.ZoneId

enum class RepeatUnit(val plural: String) { DAYS("days"), WEEKS("weeks"), MONTHS("months") }

/** "Every [every] [unit]". Deadlines are stepped in local time, so 9 AM stays 9 AM across DST. */
data class Repeat(val every: Int, val unit: RepeatUnit) {
    init { require(every >= 1) { "every must be at least 1" } }

    /** The [n]th occurrence after [anchor]. Counting from a fixed anchor avoids month-end drift (Jan 31 → Feb 28 → Mar 31). */
    fun nth(anchor: Long, n: Long, zone: ZoneId): Long {
        val start = Instant.ofEpochMilli(anchor).atZone(zone)
        val steps = n * every
        val next = when (unit) {
            RepeatUnit.DAYS -> start.plusDays(steps)
            RepeatUnit.WEEKS -> start.plusWeeks(steps)
            RepeatUnit.MONTHS -> start.plusMonths(steps)
        }
        return next.toInstant().toEpochMilli()
    }

    val label: String
        get() = when {
            every == 1 && unit == RepeatUnit.DAYS -> "Daily"
            every == 1 && unit == RepeatUnit.WEEKS -> "Weekly"
            every == 1 && unit == RepeatUnit.MONTHS -> "Monthly"
            else -> "Every $every ${unit.plural}"
        }
}
