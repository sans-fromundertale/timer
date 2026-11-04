package com.fliptimer.controller

import com.fliptimer.core.Task
import com.fliptimer.data.TaskRepository
import com.fliptimer.system.Alarms
import com.fliptimer.system.Notifications
import com.fliptimer.system.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Task changes plus their alarms. Each task holds at most one pending alarm: the next moment it
 * needs attention (see [Task.nextAlarm]). Every change also refreshes the home screen widget.
 */
class TaskController(
    private val repo: TaskRepository,
    private val alarms: Alarms,
    private val notifications: Notifications,
    private val widgets: WidgetUpdater,
    private val scope: CoroutineScope,
) {
    val tasks: Flow<List<Task>> = repo.tasks

    fun save(task: Task) {
        scope.launch {
            refresh(repo.save(task), System.currentTimeMillis())
            widgets.update()
        }
    }

    fun setProgress(task: Task, percent: Int) = save(task.withProgress(percent, System.currentTimeMillis()))

    fun delete(task: Task) {
        scope.launch {
            repo.delete(task.id)
            forget(task.id)
            widgets.update()
        }
    }

    fun clearCompleted() {
        scope.launch {
            repo.deleteCompleted().forEach(::forget)
            widgets.update()
        }
    }

    /** App open, boot, update: reset any recurring tasks whose deadline passed, and rebuild alarms. */
    suspend fun syncAll() {
        val now = System.currentTimeMillis()
        repo.all().forEach { refresh(it, now) }
        widgets.update()
    }

    suspend fun onAlarm(id: Long, scheduledFor: Long) {
        val task = repo.get(id) ?: return
        // Reminder first, so a recurring task's "due now" still shows before it resets.
        val due = task.nextReminder(scheduledFor - 1)
        if (due != null && due.at == scheduledFor) notifications.showTaskReminder(task, due.preset)
        refresh(task, maxOf(System.currentTimeMillis(), scheduledFor))
        widgets.update() // entered the due-soon window, went overdue, or reset
    }

    /** Applies any pending reset, saves it, and sets the task's next alarm. */
    private suspend fun refresh(task: Task, now: Long) {
        val current = task.rolledOver(now, ZoneId.systemDefault())
        if (current != task) repo.save(current)
        val next = current.nextAlarm(now)
        if (next != null) alarms.set(Alarms.task(current.id), next) else alarms.cancel(Alarms.task(current.id))
    }

    private fun forget(id: Long) {
        alarms.cancel(Alarms.task(id))
        notifications.cancelTask(id)
    }
}
