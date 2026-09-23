package com.fliptimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fliptimer.core.TimerState

object AlarmScheduler {
    const val EXTRA_ZERO_TIME = "zero_time"

    /** Replaces any pending alarm with one for the next zero crossing (or none). */
    fun schedule(context: Context, s: TimerState) {
        val am = context.getSystemService(AlarmManager::class.java)
        val next = s.nextZeroCrossing(System.currentTimeMillis())
        val intent = Intent(context, ZeroAlarmReceiver::class.java).putExtra(EXTRA_ZERO_TIME, next ?: -1L)
        val pi = PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        if (next == null) return

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }
}
