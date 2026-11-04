package com.fliptimer.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fliptimer.graph
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the receiver alive until it finishes. */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.graph.scope.launch {
        try { block() } finally { pending.finish() }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.data ?: return
        val scheduledFor = intent.getLongExtra(Alarms.EXTRA_SCHEDULED_FOR, -1L)
        val g = context.graph
        runAsync(context) {
            when (slot.host) {
                "timer" -> g.timer.onZeroAlarm(scheduledFor)
                "day" -> g.timer.sync()
                "task" -> slot.lastPathSegment?.toLongOrNull()?.let { g.tasks.onAlarm(it, scheduledFor) }
            }
        }
    }
}

/** Pause / Resume / Flip / Reset buttons on the timer notification. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        runAsync(context) { context.graph.timer.perform(TimerActions.transform(action)) }
    }
}

/** Alarms don't survive a reboot or app update, so rebuild them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync(context) { context.graph.syncAll() }
    }
}
