package com.fliptimer.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fliptimer.core.ReminderPreset
import com.fliptimer.core.Repeat
import com.fliptimer.core.RepeatUnit
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
    val repeatEvery: Int?,
    val repeatUnit: String?,
    val repeatAnchor: Long?,
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
        repeat = repeatEvery?.let { every ->
            RepeatUnit.entries.firstOrNull { it.name == repeatUnit }?.let { Repeat(every.coerceAtLeast(1), it) }
        },
        repeatAnchor = repeatAnchor,
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
            repeatEvery = t.repeat?.every,
            repeatUnit = t.repeat?.unit?.name,
            repeatAnchor = t.repeatAnchor,
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

    /** Finished one-off tasks. Recurring tasks are never "finished for good". */
    @Query("SELECT id FROM tasks WHERE progress >= 100 AND repeatEvery IS NULL")
    suspend fun completedIds(): List<Long>

    @Query("DELETE FROM tasks WHERE progress >= 100 AND repeatEvery IS NULL")
    suspend fun deleteCompleted()
}

@Dao
interface DayEndDao {
    @Query("SELECT * FROM day_ends ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<DayEndEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DayEndEntity>)
}

@Database(entities = [TaskEntity::class, DayEndEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun dayEndDao(): DayEndDao
}

/** v2 added repeating tasks. Existing tasks keep their data and simply don't repeat. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN repeatEvery INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN repeatUnit TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN repeatAnchor INTEGER")
    }
}
