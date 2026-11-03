package com.fliptimer.core

data class Task(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val progress: Int = 0,
    val deadline: Long? = null,
    val reminders: Set<ReminderPreset> = ReminderPreset.DEFAULT,
    val createdAt: Long = 0,
    val completedAt: Long? = null,
) {
    val isNew: Boolean get() = id == 0L
    val isComplete: Boolean get() = progress >= 100

    fun remaining(now: Long): Long? = deadline?.minus(now)

    /** Sets progress; reaching 100% stamps the completion time, dropping below clears it. */
    fun withProgress(percent: Int, now: Long): Task {
        val p = percent.coerceIn(0, 100)
        return copy(progress = p, completedAt = if (p >= 100) (completedAt ?: now) else null)
    }

    fun nextReminder(now: Long): ReminderFire? =
        if (isComplete) null else nextReminder(deadline, reminders, now)
}

object TaskLists {
    const val DUE_SOON_WINDOW_MS = 24 * 60 * 60_000L

    /** Unfinished tasks: soonest deadline first, then tasks without deadlines, oldest first. */
    fun active(tasks: List<Task>): List<Task> =
        tasks.filter { !it.isComplete }
            .sortedWith(compareBy<Task, Long?>(nullsLast()) { it.deadline }.thenBy { it.createdAt })

    /** Finished tasks, most recently finished first. */
    fun completed(tasks: List<Task>): List<Task> =
        tasks.filter { it.isComplete }.sortedByDescending { it.completedAt ?: 0L }

    /** Unfinished tasks due within 24 hours, including overdue ones. */
    fun dueSoon(tasks: List<Task>, now: Long): List<Task> =
        active(tasks).filter { t -> t.remaining(now)?.let { it <= DUE_SOON_WINDOW_MS } == true }
}
