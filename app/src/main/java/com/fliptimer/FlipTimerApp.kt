package com.fliptimer

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.fliptimer.controller.TaskController
import com.fliptimer.controller.TimerController
import com.fliptimer.data.AppDatabase
import com.fliptimer.data.DayLogRepository
import com.fliptimer.data.MIGRATION_1_2
import com.fliptimer.data.TaskRepository
import com.fliptimer.data.TimerStore
import com.fliptimer.system.Alarms
import com.fliptimer.system.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlipTimerApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        graph.notifications.createChannels()
    }
}

/** Wires every component together once. Everything else gets its dependencies from here. */
class AppGraph(app: Application) {
    /** App-wide scope for writes, so they finish even if the screen that started them closes. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val db: AppDatabase =
        Room.databaseBuilder(app, AppDatabase::class.java, "fliptimer.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    val notifications = Notifications(app)
    private val alarms = Alarms(app)
    private val timerStore = TimerStore(app)

    val dayLog = DayLogRepository(db.dayEndDao(), timerStore)
    val timer = TimerController(timerStore, dayLog, alarms, notifications, scope)
    val tasks = TaskController(TaskRepository(db.taskDao()), alarms, notifications, scope)

    /** Re-derive every alarm and notification from saved state (app open, boot, update). */
    suspend fun syncAll() {
        timer.sync()
        tasks.syncAll()
    }

    fun launchSyncAll() {
        scope.launch { syncAll() }
    }
}

val Context.graph: AppGraph get() = (applicationContext as FlipTimerApp).graph
