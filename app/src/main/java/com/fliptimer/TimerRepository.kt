package com.fliptimer

import android.content.Context
import android.content.SharedPreferences
import com.fliptimer.core.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Single source of truth, shared by the UI and the notification/alarm receivers, backed by prefs. */
object TimerRepository {
    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("timer", Context.MODE_PRIVATE)
        prefs = p
        val d = TimerState()
        _state.value = TimerState(
            initialMillis = p.getLong("initial", d.initialMillis),
            baseMillis = p.getLong("base", d.baseMillis),
            anchorMillis = p.getLong("anchor", d.anchorMillis),
            direction = p.getInt("direction", d.direction),
            running = p.getBoolean("running", d.running),
        )
    }

    /** Apply a change, persist it, then refresh the alarm and notification to match. */
    fun update(context: Context, transform: (TimerState) -> TimerState) {
        init(context)
        val new = synchronized(this) {
            transform(_state.value).also { s ->
                _state.value = s
                prefs!!.edit()
                    .putLong("initial", s.initialMillis)
                    .putLong("base", s.baseMillis)
                    .putLong("anchor", s.anchorMillis)
                    .putInt("direction", s.direction)
                    .putBoolean("running", s.running)
                    .commit()
            }
        }
        syncSideEffects(context, new)
    }

    fun syncSideEffects(context: Context, s: TimerState = state.value) {
        AlarmScheduler.schedule(context, s)
        Notifications.updateOngoing(context, s)
    }
}
