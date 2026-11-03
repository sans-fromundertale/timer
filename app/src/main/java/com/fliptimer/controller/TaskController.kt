package com.fliptimer.controller

import com.fliptimer.core.Task
import com.fliptimer.data.TaskRepository
import com.fliptimer.system.Alarms
import com.fliptimer.system.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Task changes plus their reminder alarms. Each task holds at most one pending alarm: its next reminder. */
class TaskController(
    private val repo: TaskRepository,
    private val alarms: Alarms,
    private val notifications: Notifications,
    private val scope: CoroutineScope,
) {
    val tasks: Flow<List<Task>> = repo.tasks

    fun save(task: Task) {
        scope.launch { schedule(repo.save(task)) }
    }

    fun setProgress(task: Task, percent: Int) = save(task.withProgress(percent, System.currentTimeMillis()))

    fun delete(task: Task) {
        scope.launch {
            repo.delete(task.id)
            forget(task.id)
        }
    }

    fun clearCompleted() {
        scope.launch { repo.deleteCompleted().forEach(::forget) }
    }

    suspend fun syncAll() = repo.all().forEach { schedule(it) }

    suspend fun onReminderAlarm(id: Long, scheduledFor: Long) {
        val task = repo.get(id) ?: return
        val due = task.nextReminder(scheduledFor - 1)
        if (due != null && due.at == scheduledFor) notifications.showTaskReminder(task, due.preset)
        schedule(task, after = maxOf(System.currentTimeMillis(), scheduledFor))
    }

    private fun schedule(task: Task, after: Long = System.currentTimeMillis()) {
        val next = task.nextReminder(after)
        if (next != null) alarms.set(Alarms.task(task.id), next.at) else alarms.cancel(Alarms.task(task.id))
    }

    private fun forget(id: Long) {
        alarms.cancel(Alarms.task(id))
        notifications.cancelTask(id)
    }
}
