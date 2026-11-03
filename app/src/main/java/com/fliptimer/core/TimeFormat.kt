package com.fliptimer.core

import kotlin.math.abs

/** Whole seconds for display, rounded up so a countdown reads 0:00 exactly at zero. */
fun displaySeconds(millis: Long): Long = -Math.floorDiv(-millis, 1000L)

/** Clock style: "4:05", "1:02:03", "−0:30". */
fun formatSeconds(seconds: Long): String {
    val sign = if (seconds < 0) "−" else ""
    val a = abs(seconds)
    val h = a / 3600
    val m = (a % 3600) / 60
    val s = a % 60
    return if (h > 0) "%s%d:%02d:%02d".format(sign, h, m, s) else "%s%d:%02d".format(sign, m, s)
}

fun formatMillis(millis: Long): String = formatSeconds(displaySeconds(millis))

/** Like [formatMillis] but always signed: "+1:00", "−1:00", "0:00". */
fun formatSignedMillis(millis: Long): String =
    if (displaySeconds(millis) > 0) "+" + formatMillis(millis) else formatMillis(millis)

/**
 * Deadline countdown with seconds: "2d 04:13:22", "4:13:22", "0:00:09".
 * Overdue times get a leading "−". Rounds up before the deadline so it reads 0:00:00 exactly at it.
 */
fun formatDeadlineClock(remainingMillis: Long): String {
    val secs = if (remainingMillis >= 0) displaySeconds(remainingMillis) else -remainingMillis / 1000
    val sign = if (remainingMillis < 0 && secs > 0) "−" else ""
    val d = secs / 86_400
    val h = (secs % 86_400) / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (d > 0) "%s%dd %02d:%02d:%02d".format(sign, d, h, m, s) else "%s%d:%02d:%02d".format(sign, h, m, s)
}
