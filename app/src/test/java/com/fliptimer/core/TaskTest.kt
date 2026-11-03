package com.fliptimer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskTest {
    private val hour = 3_600_000L
    private val now = 1_000_000_000L

    @Test fun defaultRemindersFireInOrder() {
        val t = Task(deadline = now + 30 * hour)
        val r1 = t.nextReminder(now)!!
        assertEquals(ReminderPreset.ONE_DAY, r1.preset)
        assertEquals(now + 6 * hour, r1.at)
        assertEquals(ReminderPreset.SIX_HOURS, t.nextReminder(r1.at)!!.preset)
        assertEquals(ReminderPreset.ONE_HOUR, t.nextReminder(now + 24 * hour)!!.preset)
        assertEquals(ReminderPreset.AT_DEADLINE, t.nextReminder(now + 29 * hour)!!.preset)
        assertNull(t.nextReminder(now + 30 * hour))
    }

    @Test fun pastRemindersSkippedWhenDeadlineIsClose() {
        val t = Task(deadline = now + 2 * hour)
        assertEquals(ReminderPreset.ONE_HOUR, t.nextReminder(now)!!.preset)
    }

    @Test fun noRemindersWithoutDeadlineOrWhenComplete() {
        assertNull(Task().nextReminder(now))
        assertNull(Task(deadline = now + 10 * hour, progress = 100).nextReminder(now))
        assertNull(Task(deadline = now + 10 * hour, reminders = emptySet()).nextReminder(now))
    }

    @Test fun completionStamping() {
        val done = Task(createdAt = 1).withProgress(100, now)
        assertEquals(now, done.completedAt)
        assertEquals(now, done.withProgress(100, now + 5).completedAt) // keeps original stamp
        assertNull(done.withProgress(80, now + 5).completedAt)
        assertEquals(100, Task().withProgress(150, now).progress)
        assertEquals(0, Task().withProgress(-3, now).progress)
    }

    @Test fun reminderEncodingRoundTrips() {
        val set = setOf(ReminderPreset.ONE_WEEK, ReminderPreset.AT_DEADLINE)
        assertEquals(set, ReminderPreset.decode(ReminderPreset.encode(set)))
        assertEquals(emptySet<ReminderPreset>(), ReminderPreset.decode(""))
        assertEquals(setOf(ReminderPreset.ONE_HOUR), ReminderPreset.decode("ONE_HOUR,BOGUS"))
    }

    @Test fun listOrdering() {
        val a = Task(id = 1, name = "no deadline old", createdAt = 1)
        val b = Task(id = 2, name = "far", deadline = now + 100 * hour, createdAt = 2)
        val c = Task(id = 3, name = "soon", deadline = now + 3 * hour, createdAt = 3)
        val d = Task(id = 4, name = "overdue", deadline = now - hour, createdAt = 4)
        val e = Task(id = 5, name = "done", deadline = now + hour, progress = 100, completedAt = now)
        val f = Task(id = 6, name = "no deadline new", createdAt = 6)
        val all = listOf(a, b, c, d, e, f)
        assertEquals(listOf(4L, 3L, 2L, 1L, 6L), TaskLists.active(all).map { it.id })
        assertEquals(listOf(5L), TaskLists.completed(all).map { it.id })
        assertEquals(listOf(4L, 3L), TaskLists.dueSoon(all, now).map { it.id })
    }
}
