package com.fliptimer.controller

import com.fliptimer.core.TimerState
import com.fliptimer.core.nextMidnight
import com.fliptimer.data.DayLogRepository
import com.fliptimer.data.TimerStore
import com.fliptimer.system.Alarms
import com.fliptimer.system.Notifications
import com.fliptimer.system.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** The only place the timer changes. Every change: log finished days, save, then refresh alarms and notification. */
class TimerController(
    private val store: TimerStore,
    private val dayLog: DayLogRepository,
    private val alarms: Alarms,
    private val notifications: Notifications,
    private val widgets: WidgetUpdater,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    val state: StateFlow<TimerState> = store.state

    suspend fun perform(change: (TimerState, Long) -> TimerState) = mutex.withLock {
        val now = System.currentTimeMillis()
        val old = store.state.value
        dayLog.catchUp(old, now) // must use the old state: it describes the time before this change
        val new = change(old, now)
        if (new != old) store.save(new)
        refresh(new, now)
    }

    /** Fire-and-forget version for the UI. */
    fun dispatch(change: (TimerState, Long) -> TimerState) {
        scope.launch { perform(change) }
    }

    suspend fun sync() = perform { s, _ -> s }

    suspend fun onZeroAlarm(scheduledFor: Long) {
        if (store.state.value.zeroTime() != scheduledFor) return
        notifications.showZeroAlert(store.state.value)
        widgets.update() // the widget's clock turns red / back to normal at zero
    }

    private suspend fun refresh(s: TimerState, now: Long) {
        val zero = s.nextZeroCrossing(now)
        if (zero != null) alarms.set(Alarms.TIMER_ZERO, zero) else alarms.cancel(Alarms.TIMER_ZERO)
        // Inexact is fine: the logged value is computed for exact midnight whenever this fires.
        alarms.set(Alarms.DAY_END, nextMidnight(now, ZoneId.systemDefault()), exact = false)
        notifications.showTimer(s)
        widgets.update()
    }
}
