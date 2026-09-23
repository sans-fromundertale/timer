package com.fliptimer

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
import com.fliptimer.core.TimerState
import com.fliptimer.core.formatMillis

object Notifications {
    private const val CH_TIMER = "timer"
    private const val CH_ZERO = "zero_v1"
    private const val ID_ONGOING = 1
    private const val ID_ZERO = 2

    const val ACTION_PAUSE = "com.fliptimer.PAUSE"
    const val ACTION_RESUME = "com.fliptimer.RESUME"
    const val ACTION_FLIP = "com.fliptimer.FLIP"
    const val ACTION_RESET = "com.fliptimer.RESET"

    private val VIBRATION = longArrayOf(0, 400, 200, 400, 200, 400)

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Running timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the live timer while it's running or paused"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
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
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun action(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, ActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun updateOngoing(context: Context, s: TimerState) {
        val nm = NotificationManagerCompat.from(context)
        if (s.isAtRest) { nm.cancel(ID_ONGOING); return }
        if (!canPost(context)) return

        val now = System.currentTimeMillis()
        val downLabel = if (s.direction == TimerState.DOWN) "▼ Counting down" else "▲ Counting up"

        val views = RemoteViews(context.packageName, R.layout.notification_timer)
        if (s.running) {
            // Chronometer works in elapsedRealtime; translate the wall-clock zero time.
            val zeroElapsed = SystemClock.elapsedRealtime() + (s.zeroTime()!! - now)
            views.setViewVisibility(R.id.chrono, View.VISIBLE)
            views.setViewVisibility(R.id.paused_time, View.GONE)
            views.setChronometer(R.id.chrono, zeroElapsed, null, true)
            views.setChronometerCountDown(R.id.chrono, s.direction == TimerState.DOWN)
            views.setTextViewText(R.id.status, downLabel)
        } else {
            views.setViewVisibility(R.id.chrono, View.GONE)
            views.setViewVisibility(R.id.paused_time, View.VISIBLE)
            views.setTextViewText(R.id.paused_time, formatMillis(s.baseMillis))
            views.setTextViewText(R.id.status, "Paused · " + downLabel.substring(2).lowercase())
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
            .setContentIntent(openAppIntent(context))

        if (s.running) b.addAction(0, "Pause", action(context, ACTION_PAUSE, 10))
        else b.addAction(0, "Resume", action(context, ACTION_RESUME, 11))
        b.addAction(0, "Flip", action(context, ACTION_FLIP, 12))
        b.addAction(0, "Reset", action(context, ACTION_RESET, 13))

        try { nm.notify(ID_ONGOING, b.build()) } catch (_: SecurityException) {}
    }

    fun showZeroAlert(context: Context, s: TimerState) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, CH_ZERO)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer hit 0:00")
            .setContentText(
                if (s.direction == TimerState.DOWN) "Still counting down, now in overtime"
                else "Still counting up"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openAppIntent(context))
            .build()
        try { NotificationManagerCompat.from(context).notify(ID_ZERO, n) } catch (_: SecurityException) {}
    }
}
