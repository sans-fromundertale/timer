package com.fliptimer.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.fliptimer.core.ReminderPreset
import com.fliptimer.core.Task
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val description: String,
    val progress: Int,
    val deadline: Long?,
    val reminders: String,
    val createdAt: Long,
    val completedAt: Long?,
) {
    fun toTask() = Task(
        id = id,
        name = name,
        description = description,
        progress = progress,
        deadline = deadline,
        reminders = ReminderPreset.decode(reminders),
        createdAt = createdAt,
        completedAt = completedAt,
    )

    companion object {
        fun from(t: Task) = TaskEntity(
            id = t.id,
            name = t.name,
            description = t.description,
            progress = t.progress,
            deadline = t.deadline,
            reminders = ReminderPreset.encode(t.reminders),
            createdAt = t.createdAt,
            completedAt = t.completedAt,
        )
    }
}

@Entity(tableName = "day_ends")
data class DayEndEntity(
    @PrimaryKey val epochDay: Long,
    val valueMillis: Long,
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    /** Inserts when id is 0 (returns the new id), otherwise replaces. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT id FROM tasks WHERE progress >= 100")
    suspend fun completedIds(): List<Long>

    @Query("DELETE FROM tasks WHERE progress >= 100")
    suspend fun deleteCompleted()
}

@Dao
interface DayEndDao {
    @Query("SELECT * FROM day_ends ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<DayEndEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DayEndEntity>)
}

@Database(entities = [TaskEntity::class, DayEndEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun dayEndDao(): DayEndDao
}
