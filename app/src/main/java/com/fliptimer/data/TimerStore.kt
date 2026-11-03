package com.fliptimer.data

import android.content.Context
import com.fliptimer.core.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The timer is a single small record that receivers must read instantly, so it lives in
 * SharedPreferences rather than the database. Keys match version 1, so saved state carries over.
 */
class TimerStore(context: Context) {
    private val prefs = context.getSharedPreferences("timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private fun load(): TimerState {
        val d = TimerState()
        return TimerState(
            initialMillis = prefs.getLong("initial", d.initialMillis),
            baseMillis = prefs.getLong("base", d.baseMillis),
            anchorMillis = prefs.getLong("anchor", d.anchorMillis),
            direction = prefs.getInt("direction", d.direction),
            running = prefs.getBoolean("running", d.running),
        )
    }

    fun save(s: TimerState) {
        prefs.edit()
            .putLong("initial", s.initialMillis)
            .putLong("base", s.baseMillis)
            .putLong("anchor", s.anchorMillis)
            .putInt("direction", s.direction)
            .putBoolean("running", s.running)
            .commit()
        _state.value = s
    }

    /** Last day whose ending value has been written to the day log, as an epoch day. */
    var lastLoggedEpochDay: Long?
        get() = prefs.getLong("last_logged_day", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        set(v) {
            prefs.edit().putLong("last_logged_day", v ?: Long.MIN_VALUE).commit()
        }
}
