package com.fliptimer.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fliptimer.R
import com.fliptimer.core.ReminderPreset
import com.fliptimer.core.Task
import com.fliptimer.core.TimerState
import com.fliptimer.core.formatMillis
import com.fliptimer.ui.MainActivity
import com.fliptimer.ui.Tab

class Notifications(private val context: Context) {
    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val sys = context.getSystemService(NotificationManager::class.java)
        sys.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Running timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the live timer while it's running or paused"
                setShowBadge(false)
            }
        )
        sys.createNotificationChannel(
            NotificationChannel(CH_ZERO, "Zero alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sound and vibration each time the timer hits 0:00"
                enableVibration(true)
                vibrationPattern = VIBRATION
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        )
        sys.createNotificationChannel(
            NotificationChannel(CH_TASKS, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders before a task's deadline"
            }
        )
    }

    // ---- Timer ----

    fun showTimer(s: TimerState) {
        if (s.isAtRest) { nm.cancel(ID_TIMER); return }
        if (!canPost(context)) return

        val now = System.currentTimeMillis()
        val dirLabel = if (s.direction == TimerState.DOWN) "Counting down" else "Counting up"
        val views = RemoteViews(context.packageName, R.layout.notification_timer)
        if (s.running) {
            // Chronometer works in elapsedRealtime; translate the wall-clock zero time.
            val zeroElapsed = SystemClock.elapsedRealtime() + (s.zeroTime()!! - now)
            views.setViewVisibility(R.id.chrono, View.VISIBLE)
            views.setViewVisibility(R.id.paused_time, View.GONE)
            views.setChronometer(R.id.chrono, zeroElapsed, null, true)
            views.setChronometerCountDown(R.id.chrono, s.direction == TimerState.DOWN)
            views.setTextViewText(R.id.status, (if (s.direction == TimerState.DOWN) "▼ " else "▲ ") + dirLabel)
        } else {
            views.setViewVisibility(R.id.chrono, View.GONE)
            views.setViewVisibility(R.id.paused_time, View.VISIBLE)
            views.setTextViewText(R.id.paused_time, formatMillis(s.baseMillis))
            views.setTextViewText(R.id.status, "Paused · ${dirLabel.lowercase()}")
        }

        val b = NotificationCompat.Builder(context, CH_TIMER)
            .setSmallIcon(R.drawable.ic_timer)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(Tab.Timer))

        if (s.running) b.addAction(0, "Pause", timerAction(ACTION_PAUSE))
        else b.addAction(0, "Resume", timerAction(ACTION_RESUME))
        b.addAction(0, "Flip", timerAction(ACTION_FLIP))
        b.addAction(0, "Reset", timerAction(ACTION_RESET))
        post(ID_TIMER, b)
    }

    fun showZeroAlert(s: TimerState) {
        post(
            ID_ZERO,
            NotificationCompat.Builder(context, CH_ZERO)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Timer hit 0:00")
                .setContentText(if (s.direction == TimerState.DOWN) "Now counting into negative time" else "Now counting into positive time")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(VIBRATION)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .setContentIntent(openApp(Tab.Timer))
        )
    }

    // ---- Tasks ----

    fun showTaskReminder(task: Task, preset: ReminderPreset) {
        post(
            taskId(task.id),
            NotificationCompat.Builder(context, CH_TASKS)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("${task.name} ${preset.notificationText}")
                .setContentText("${task.progress}% done")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openApp(Tab.Tasks))
        )
    }

    fun cancelTask(id: Long) = nm.cancel(taskId(id))

    // ---- Helpers ----

    private fun post(id: Int, b: NotificationCompat.Builder) {
        if (!canPost(context)) return
        try { nm.notify(id, b.build()) } catch (_: SecurityException) {}
    }

    private fun taskId(id: Long): Int = ID_TASK_BASE + (id % 1_000_000).toInt()

    private fun openApp(tab: Tab): PendingIntent =
        PendingIntent.getActivity(
            context, tab.ordinal,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, tab.name)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun timerAction(action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, action.hashCode(),
            Intent(context, TimerActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val CH_TIMER = "timer"
        private const val CH_ZERO = "zero_v1"
        private const val CH_TASKS = "tasks"
        private const val ID_TIMER = 1
        private const val ID_ZERO = 2
        private const val ID_TASK_BASE = 1000

        private const val ACTION_PAUSE = "com.fliptimer.PAUSE"
        private const val ACTION_RESUME = "com.fliptimer.RESUME"
        private const val ACTION_FLIP = "com.fliptimer.FLIP"
        private const val ACTION_RESET = "com.fliptimer.RESET"

        private val VIBRATION = longArrayOf(0, 400, 200, 400, 200, 400)

        fun canPost(context: Context): Boolean =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        /** Maps a notification button to the timer change it makes. */
        fun timerTransform(action: String): (TimerState, Long) -> TimerState = { s, now ->
            when (action) {
                ACTION_PAUSE -> s.pause(now)
                ACTION_RESUME -> s.start(now)
                ACTION_FLIP -> s.flip(now)
                ACTION_RESET -> s.reset()
                else -> s
            }
        }
    }
}
