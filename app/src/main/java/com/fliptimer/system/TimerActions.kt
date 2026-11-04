package com.fliptimer.system

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.fliptimer.core.TimerState
import com.fliptimer.ui.MainActivity
import com.fliptimer.ui.Tab

/** Timer buttons and app-opening intents shared by the notification and the home screen widget. */
object TimerActions {
    const val PAUSE = "com.fliptimer.PAUSE"
    const val RESUME = "com.fliptimer.RESUME"
    const val START_PAUSE = "com.fliptimer.START_PAUSE"
    const val FLIP = "com.fliptimer.FLIP"
    const val RESET = "com.fliptimer.RESET"

    /** Maps a button to the timer change it makes. */
    fun transform(action: String): (TimerState, Long) -> TimerState = { s, now ->
        when (action) {
            PAUSE -> s.pause(now)
            RESUME -> s.start(now)
            START_PAUSE -> if (s.running) s.pause(now) else s.start(now)
            FLIP -> s.flip(now)
            RESET -> s.reset()
            else -> s
        }
    }

    fun pendingIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, action.hashCode(),
            Intent(context, TimerActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun openApp(context: Context, tab: Tab): PendingIntent =
        PendingIntent.getActivity(
            context, tab.ordinal,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, tab.name)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Chronometer views tick in elapsedRealtime; converts a wall-clock moment to that clock. */
    fun chronometerBase(wallClockMillis: Long, now: Long = System.currentTimeMillis()): Long =
        SystemClock.elapsedRealtime() + (wallClockMillis - now)
}
