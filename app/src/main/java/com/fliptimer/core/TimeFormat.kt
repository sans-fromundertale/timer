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

/** Human duration for deadlines: "2d 4h", "5h 12m", "12m 30s", "45s". Negative is treated as 0. */
fun formatDuration(millis: Long): String {
    val total = maxOf(0L, millis) / 1000
    val d = total / 86_400
    val h = (total % 86_400) / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

/** "5h 12m" before the deadline, "overdue 3h 4m" after it. */
fun formatCountdown(remainingMillis: Long): String =
    if (remainingMillis >= 0) formatDuration(remainingMillis) else "overdue ${formatDuration(-remainingMillis)}"
