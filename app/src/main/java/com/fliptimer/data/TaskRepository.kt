package com.fliptimer.data

import com.fliptimer.core.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val dao: TaskDao) {
    val tasks: Flow<List<Task>> = dao.observeAll().map { list -> list.map(TaskEntity::toTask) }

    suspend fun all(): List<Task> = dao.getAll().map(TaskEntity::toTask)

    suspend fun get(id: Long): Task? = dao.get(id)?.toTask()

    /** Saves and returns the task with its real id. */
    suspend fun save(task: Task): Task {
        val id = dao.upsert(TaskEntity.from(task))
        return if (task.isNew) task.copy(id = id) else task
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** Deletes completed tasks and returns their ids. */
    suspend fun deleteCompleted(): List<Long> {
        val ids = dao.completedIds()
        dao.deleteCompleted()
        return ids
    }
}
