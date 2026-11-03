package com.fliptimer.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/** One alarm slot per URI: setting it again replaces the previous one. All alarms go to [AlarmReceiver]. */
class Alarms(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    fun set(slot: Uri, at: Long, exact: Boolean = true) {
        val pi = pending(slot, at)
        val canExact = exact && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms())
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(slot: Uri) = am.cancel(pending(slot, -1L))

    private fun pending(slot: Uri, at: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setData(slot).putExtra(EXTRA_SCHEDULED_FOR, at),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        /** The time an alarm was set for, so a receiver can tell if it's stale. */
        const val EXTRA_SCHEDULED_FOR = "scheduled_for"

        val TIMER_ZERO: Uri = Uri.parse("fliptimer://timer/zero")
        val DAY_END: Uri = Uri.parse("fliptimer://day/end")
        fun task(id: Long): Uri = Uri.parse("fliptimer://task/$id")
    }
}
