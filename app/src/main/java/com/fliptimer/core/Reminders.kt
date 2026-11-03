package com.fliptimer.core

private const val MIN_MS = 60_000L
private const val HOUR_MS = 60 * MIN_MS
private const val DAY_MS = 24 * HOUR_MS

/** Reminder times offered per task, as offsets before the deadline. */
enum class ReminderPreset(val offsetMillis: Long, private val shortLabel: String) {
    ONE_WEEK(7 * DAY_MS, "1 week"),
    THREE_DAYS(3 * DAY_MS, "3 days"),
    ONE_DAY(DAY_MS, "24 hours"),
    SIX_HOURS(6 * HOUR_MS, "6 hours"),
    ONE_HOUR(HOUR_MS, "1 hour"),
    FIFTEEN_MINUTES(15 * MIN_MS, "15 minutes"),
    AT_DEADLINE(0L, "");

    val menuLabel: String get() = if (this == AT_DEADLINE) "At the deadline" else "$shortLabel before"
    val notificationText: String get() = if (this == AT_DEADLINE) "is due now" else "is due in $shortLabel"

    companion object {
        val DEFAULT: Set<ReminderPreset> = setOf(ONE_DAY, SIX_HOURS, ONE_HOUR, AT_DEADLINE)

        fun encode(set: Set<ReminderPreset>): String = set.joinToString(",") { it.name }
        fun decode(s: String): Set<ReminderPreset> =
            s.split(",").mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
    }
}

data class ReminderFire(val at: Long, val preset: ReminderPreset)

/** The earliest reminder strictly after [now], or null if none are left. */
fun nextReminder(deadline: Long?, presets: Set<ReminderPreset>, now: Long): ReminderFire? {
    if (deadline == null) return null
    return presets
        .map { ReminderFire(deadline - it.offsetMillis, it) }
        .filter { it.at > now }
        .minByOrNull { it.at }
}
