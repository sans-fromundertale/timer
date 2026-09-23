package com.fliptimer.core

import kotlin.math.abs

/**
 * The whole timer as immutable data. Pure Kotlin, no Android, so it can be unit-tested.
 *
 * The displayed value is a line: value(t) = baseMillis + direction * (t - anchorMillis).
 * Every action (start, pause, flip) "re-anchors" the line at the current value,
 * so nothing ever jumps or resets. Times are wall-clock millis so state survives reboots.
 */
data class TimerState(
    val initialMillis: Long = 5 * 60_000L,
    val baseMillis: Long = 5 * 60_000L,
    val anchorMillis: Long = 0L,
    val direction: Int = DOWN,
    val running: Boolean = false,
) {
    companion object {
        const val UP = 1
        const val DOWN = -1
    }

    fun valueAt(now: Long): Long =
        if (running) baseMillis + direction * (now - anchorMillis) else baseMillis

    /** Wall-clock time at which the current run line is at zero (past or future). Null when paused. */
    fun zeroTime(): Long? = if (running) anchorMillis - direction * baseMillis else null

    /** The next zero crossing strictly in the future, or null if the timer isn't heading toward zero. */
    fun nextZeroCrossing(now: Long): Long? = zeroTime()?.takeIf { it > now }

    fun start(now: Long): TimerState = if (running) this else copy(anchorMillis = now, running = true)

    fun pause(now: Long): TimerState = if (!running) this else copy(baseMillis = valueAt(now), running = false)

    fun flip(now: Long): TimerState =
        copy(baseMillis = valueAt(now), anchorMillis = now, direction = -direction)

    fun reset(): TimerState = copy(baseMillis = initialMillis, running = false)

    fun setInitial(millis: Long): TimerState =
        copy(initialMillis = millis, baseMillis = millis, running = false)

    /** Paused at the starting value: nothing worth showing in a notification. */
    val isAtRest: Boolean get() = !running && baseMillis == initialMillis
}

/** Whole seconds for display, rounded up so a countdown reads 0:00 exactly at zero. */
fun displaySeconds(millis: Long): Long = -Math.floorDiv(-millis, 1000L)

fun formatSeconds(seconds: Long): String {
    val sign = if (seconds < 0) "−" else ""
    val a = abs(seconds)
    val h = a / 3600
    val m = (a % 3600) / 60
    val s = a % 60
    return if (h > 0) "%s%d:%02d:%02d".format(sign, h, m, s) else "%s%d:%02d".format(sign, m, s)
}

fun formatMillis(millis: Long): String = formatSeconds(displaySeconds(millis))
