package com.fliptimer.system

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.fliptimer.R
import com.fliptimer.core.Task
import com.fliptimer.core.TaskLists
import com.fliptimer.core.TimerState
import com.fliptimer.core.formatMillis
import com.fliptimer.graph
import com.fliptimer.ui.Tab
import kotlinx.coroutines.launch

/** The home screen widget. Android redraws its clocks itself, so it only needs updating when something changes. */
class TimerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        context.graph.scope.launch {
            try { context.graph.widgets.update() } finally { pending.finish() }
        }
    }
}

class WidgetUpdater(
    private val context: Context,
    private val timerState: () -> TimerState,
    private val tasks: suspend () -> List<Task>,
) {
    suspend fun update() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidget::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(timerState(), tasks(), System.currentTimeMillis()))
    }

    private fun build(s: TimerState, allTasks: List<Task>, now: Long): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_timer)
        val accent = context.getColor(R.color.widget_accent)
        val error = context.getColor(R.color.widget_error)
        val value = s.valueAt(now)

        v.setTextViewText(R.id.w_direction, if (s.direction == TimerState.DOWN) "▼ Counting down" else "▲ Counting up")
        if (s.running) {
            v.setViewVisibility(R.id.w_chrono, View.VISIBLE)
            v.setViewVisibility(R.id.w_paused, View.GONE)
            v.setChronometer(R.id.w_chrono, TimerActions.chronometerBase(s.zeroTime()!!, now), null, true)
            v.setChronometerCountDown(R.id.w_chrono, s.direction == TimerState.DOWN)
            // Refreshed at each zero crossing, so the colour stays right.
            v.setTextColor(R.id.w_chrono, if (value < 0) error else context.getColor(R.color.widget_text))
        } else {
            v.setViewVisibility(R.id.w_chrono, View.GONE)
            v.setViewVisibility(R.id.w_paused, View.VISIBLE)
            v.setTextViewText(R.id.w_paused, formatMillis(s.baseMillis))
            v.setTextColor(R.id.w_paused, if (value < 0) error else context.getColor(R.color.widget_text))
        }

        v.setTextViewText(R.id.w_start_pause, if (s.running) "Pause" else "Start")
        v.setOnClickPendingIntent(R.id.w_start_pause, TimerActions.pendingIntent(context, TimerActions.START_PAUSE))
        v.setOnClickPendingIntent(R.id.w_flip, TimerActions.pendingIntent(context, TimerActions.FLIP))
        v.setOnClickPendingIntent(R.id.w_reset, TimerActions.pendingIntent(context, TimerActions.RESET))
        v.setOnClickPendingIntent(R.id.w_timer_area, TimerActions.openApp(context, Tab.Timer))

        val due = TaskLists.dueSoon(allTasks, now)
        v.setViewVisibility(R.id.w_due_header, if (due.isEmpty()) View.GONE else View.VISIBLE)
        TASK_ROWS.forEachIndexed { i, (row, name, clock) ->
            val t = due.getOrNull(i)
            if (t == null) {
                v.setViewVisibility(row, View.GONE)
                return@forEachIndexed
            }
            val deadline = t.deadline!!
            v.setViewVisibility(row, View.VISIBLE)
            v.setTextViewText(name, t.name)
            v.setChronometer(clock, TimerActions.chronometerBase(deadline, now), null, true)
            v.setChronometerCountDown(clock, true)
            v.setTextColor(clock, if (deadline <= now) error else accent)
            v.setOnClickPendingIntent(row, TimerActions.openApp(context, Tab.Tasks))
        }
        val extra = due.size - TASK_ROWS.size
        v.setViewVisibility(R.id.w_more, if (extra > 0) View.VISIBLE else View.GONE)
        v.setTextViewText(R.id.w_more, "+$extra more")
        v.setOnClickPendingIntent(R.id.w_more, TimerActions.openApp(context, Tab.Tasks))
        return v
    }

    private companion object {
        val TASK_ROWS = listOf(
            Triple(R.id.w_task1, R.id.w_task1_name, R.id.w_task1_clock),
            Triple(R.id.w_task2, R.id.w_task2_name, R.id.w_task2_clock),
            Triple(R.id.w_task3, R.id.w_task3_name, R.id.w_task3_clock),
        )
    }
}
