package com.fliptimer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RepeatTest {
    private val zone = ZoneId.of("America/Toronto")
    private fun at(y: Int, mo: Int, d: Int, h: Int = 9, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private val weekly = Repeat(1, RepeatUnit.WEEKS)

    @Test fun labels() {
        assertEquals("Daily", Repeat(1, RepeatUnit.DAYS).label)
        assertEquals("Weekly", weekly.label)
        assertEquals("Every 2 weeks", Repeat(2, RepeatUnit.WEEKS).label)
        assertEquals("Every 3 months", Repeat(3, RepeatUnit.MONTHS).label)
    }

    @Test fun dailyKeepsLocalTimeAcrossDst() {
        // DST ends Nov 1 2026 in Toronto
        assertEquals(at(2026, 11, 2), Repeat(1, RepeatUnit.DAYS).nth(at(2026, 10, 31), 2, zone))
    }

    @Test fun monthlyFromMonthEndDoesNotDrift() {
        val r = Repeat(1, RepeatUnit.MONTHS)
        val jan31 = at(2027, 1, 31)
        assertEquals(at(2027, 2, 28), r.nth(jan31, 1, zone))
        assertEquals(at(2027, 3, 31), r.nth(jan31, 2, zone))
    }

    @Test fun completedTaskStaysDoneUntilDeadlineThenResets() {
        val due = at(2026, 11, 6)
        val t = Task(deadline = due, createdAt = 1).withSchedule(due, weekly).withProgress(100, at(2026, 11, 4))
        assertEquals(t, t.rolledOver(due - 1, zone)) // still done before the deadline
        val next = t.rolledOver(due, zone)
        assertEquals(at(2026, 11, 13), next.deadline)
        assertEquals(0, next.progress)
        assertNull(next.completedAt)
    }

    @Test fun unfinishedTaskAlsoRollsOver() {
        val due = at(2026, 11, 6)
        val t = Task().withSchedule(due, weekly).withProgress(40, 0)
        assertEquals(at(2026, 11, 13), t.rolledOver(due + 1000, zone).deadline)
    }

    @Test fun skipsMissedPeriodsAndStaysOnSchedule() {
        val due = at(2026, 11, 6)
        val t = Task().withSchedule(due, weekly)
        val later = t.rolledOver(at(2026, 11, 25), zone) // phone off for weeks
        assertEquals(at(2026, 11, 27), later.deadline)
        assertEquals(due, later.repeatAnchor)
    }

    @Test fun nonRecurringNeverRollsOver() {
        val t = Task(deadline = at(2026, 11, 6))
        assertEquals(t, t.rolledOver(at(2027, 1, 1), zone))
    }

    @Test fun repeatNeedsDeadline() {
        val t = Task().withSchedule(null, weekly)
        assertNull(t.repeat)
        assertNull(t.repeatAnchor)
    }

    @Test fun nextAlarmIncludesDeadlineForRecurring() {
        val due = at(2026, 11, 6)
        val done = Task().withSchedule(due, weekly).withProgress(100, 0)
        assertEquals(due, done.nextAlarm(at(2026, 11, 1))) // no reminders while done, but still wakes to reset
        val oneOff = Task(deadline = due).withProgress(100, 0)
        assertNull(oneOff.nextAlarm(at(2026, 11, 1)))
    }

    @Test fun listsKeepDoneRecurringTasksActive() {
        val now = at(2026, 11, 6, 0)
        val doneRecurring = Task(id = 1, createdAt = 1).withSchedule(now + 3_600_000, weekly).withProgress(100, now)
        val doneOneOff = Task(id = 2, createdAt = 2, deadline = now + 3_600_000).withProgress(100, now)
        val open = Task(id = 3, createdAt = 3, deadline = now + 7_200_000)
        val all = listOf(doneRecurring, doneOneOff, open)
        assertEquals(listOf(3L, 1L), TaskLists.active(all).map { it.id }) // unfinished first
        assertEquals(listOf(2L), TaskLists.completed(all).map { it.id })
        assertEquals(listOf(3L), TaskLists.dueSoon(all, now).map { it.id })
    }
}
