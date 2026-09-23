package com.fliptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Pause / Resume / Flip / Reset buttons in the notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        TimerRepository.update(context) { s ->
            when (intent.action) {
                Notifications.ACTION_PAUSE -> s.pause(now)
                Notifications.ACTION_RESUME -> s.start(now)
                Notifications.ACTION_FLIP -> s.flip(now)
                Notifications.ACTION_RESET -> s.reset()
                else -> s
            }
        }
    }
}

/** Fires at a zero crossing. Ignored if the timer changed since the alarm was set. */
class ZeroAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerRepository.init(context)
        val expected = intent.getLongExtra(AlarmScheduler.EXTRA_ZERO_TIME, -1L)
        val s = TimerRepository.state.value
        if (expected == -1L || s.zeroTime() != expected) return
        Notifications.showZeroAlert(context, s)
    }
}

/** Restore the alarm and notification after a reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerRepository.init(context)
        Notifications.createChannels(context)
        TimerRepository.syncSideEffects(context)
    }
}
