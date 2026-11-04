package com.fliptimer.core

import java.time.ZoneId

data class Task(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val progress: Int = 0,
    val deadline: Long? = null,
    val reminders: Set<ReminderPreset> = ReminderPreset.DEFAULT,
    val createdAt: Long = 0,
    val completedAt: Long? = null,
    val repeat: Repeat? = null,
    /** The deadline the repeat schedule counts from. Reset whenever the deadline is edited. */
    val repeatAnchor: Long? = null,
) {
    val isNew: Boolean get() = id == 0L
    val isComplete: Boolean get() = progress >= 100
    val isRecurring: Boolean get() = repeat != null && deadline != null

    fun remaining(now: Long): Long? = deadline?.minus(now)

    /** Sets progress; reaching 100% stamps the completion time, dropping below clears it. */
    fun withProgress(percent: Int, now: Long): Task {
        val p = percent.coerceIn(0, 100)
        return copy(progress = p, completedAt = if (p >= 100) (completedAt ?: now) else null)
    }

    /** Sets deadline and repeat together; a repeat needs a deadline, and editing either restarts the schedule. */
    fun withSchedule(deadline: Long?, repeat: Repeat?): Task {
        val r = if (deadline == null) null else repeat
        return copy(deadline = deadline, repeat = r, repeatAnchor = if (r != null) deadline else null)
    }

    fun nextReminder(now: Long): ReminderFire? =
        if (isComplete) null else nextReminder(deadline, reminders, now)

    /**
     * When this task next needs attention: its next reminder, entering the 24-hour "due soon"
     * window, or its deadline (to turn overdue, or for a recurring task, to reset).
     */
    fun nextAlarm(now: Long): Long? {
        val d = deadline
        val candidates = listOfNotNull(
            nextReminder(now)?.at,
            d?.takeIf { !isComplete || isRecurring },
            d?.minus(TaskLists.DUE_SOON_WINDOW_MS)?.takeIf { !isComplete },
        )
        return candidates.filter { it > now }.minOrNull()
    }

    /**
     * Once a recurring task's deadline passes, it starts over: progress back to 0
     * and the deadline moved to the next occurrence after [now] (skipping any missed ones).
     */
    fun rolledOver(now: Long, zone: ZoneId): Task {
        val r = repeat ?: return this
        val d = deadline ?: return this
        if (d > now) return this
        val anchor = repeatAnchor ?: d
        var n = 1L
        var next = r.nth(anchor, n, zone)
        while (next <= now && n < 100_000) next = r.nth(anchor, ++n, zone)
        return copy(deadline = next, repeatAnchor = anchor, progress = 0, completedAt = null)
    }
}

object TaskLists {
    const val DUE_SOON_WINDOW_MS = 24 * 60 * 60_000L

    /**
     * The main list: unfinished tasks plus recurring tasks (finished ones stay here until they reset).
     * Unfinished before finished, then soonest deadline, then tasks without deadlines, oldest first.
     */
    fun active(tasks: List<Task>): List<Task> =
        tasks.filter { !it.isComplete || it.isRecurring }
            .sortedWith(
                compareBy<Task> { it.isComplete }
                    .thenBy(nullsLast()) { it.deadline }
                    .thenBy { it.createdAt }
            )

    /** Finished one-off tasks, most recently finished first. */
    fun completed(tasks: List<Task>): List<Task> =
        tasks.filter { it.isComplete && !it.isRecurring }.sortedByDescending { it.completedAt ?: 0L }

    /** Unfinished tasks due within 24 hours, including overdue ones. */
    fun dueSoon(tasks: List<Task>, now: Long): List<Task> =
        active(tasks).filter { t -> !t.isComplete && t.remaining(now)?.let { it <= DUE_SOON_WINDOW_MS } == true }
}
