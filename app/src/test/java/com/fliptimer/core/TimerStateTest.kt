package com.fliptimer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerStateTest {
    private val t0 = 1_000_000L
    private val fiveMin = 5 * 60_000L

    @Test fun countsDown() {
        val s = TimerState().setInitial(fiveMin).start(t0)
        assertEquals(fiveMin - 10_000, s.valueAt(t0 + 10_000))
    }

    @Test fun flipKeepsValueAndReverses() {
        val s = TimerState().setInitial(fiveMin).start(t0).flip(t0 + 60_000)
        assertEquals(4 * 60_000L, s.valueAt(t0 + 60_000))
        assertEquals(4 * 60_000L + 30_000, s.valueAt(t0 + 90_000))
        assertEquals(TimerState.UP, s.direction)
    }

    @Test fun keepsRunningPastZero() {
        val s = TimerState().setInitial(10_000).start(t0)
        assertEquals(-5_000L, s.valueAt(t0 + 15_000))
    }

    @Test fun nextCrossingWhenCountingDown() {
        val s = TimerState().setInitial(10_000).start(t0)
        assertEquals(t0 + 10_000, s.nextZeroCrossing(t0))
        assertNull(s.nextZeroCrossing(t0 + 10_000)) // already crossed
    }

    @Test fun noCrossingWhenMovingAway() {
        assertNull(TimerState(direction = TimerState.UP).setInitial(10_000).start(t0).nextZeroCrossing(t0))
        assertNull(TimerState().setInitial(10_000).nextZeroCrossing(t0)) // paused
    }

    @Test fun flippingAfterZeroCrossesAgain() {
        // down from 10s, at t+15 it's -5s, flip up -> crosses zero again 5s later
        val s = TimerState().setInitial(10_000).start(t0).flip(t0 + 15_000)
        assertEquals(t0 + 20_000, s.nextZeroCrossing(t0 + 15_000))
        assertEquals(0L, s.valueAt(t0 + 20_000))
    }

    @Test fun pauseFreezesAndResumeContinues() {
        val p = TimerState().setInitial(fiveMin).start(t0).pause(t0 + 20_000)
        assertEquals(fiveMin - 20_000, p.valueAt(t0 + 999_999))
        val r = p.start(t0 + 100_000)
        assertEquals(fiveMin - 25_000, r.valueAt(t0 + 105_000))
    }

    @Test fun flipWhilePausedThenResume() {
        val s = TimerState().setInitial(10_000).start(t0).pause(t0 + 4_000).flip(t0 + 50_000).start(t0 + 60_000)
        assertEquals(8_000L, s.valueAt(t0 + 62_000))
    }

    @Test fun zeroTimeMatchesValueZeroBothDirections() {
        val down = TimerState().setInitial(7_000).start(t0)
        assertEquals(0L, down.valueAt(down.zeroTime()!!))
        val up = TimerState().setInitial(7_000).start(t0).flip(t0 + 20_000) // at -13s going up
        assertEquals(0L, up.valueAt(up.zeroTime()!!))
    }

    @Test fun resetRestoresInitialKeepsDirection() {
        val s = TimerState().setInitial(fiveMin).start(t0).flip(t0 + 1_000).reset()
        assertEquals(fiveMin, s.baseMillis)
        assertEquals(TimerState.UP, s.direction)
        assertEquals(true, s.isAtRest)
    }

    @Test fun negativeStartCountsTowardZero() {
        val s = TimerState(direction = TimerState.UP).setInitial(-60_000).start(t0)
        assertEquals(-30_000L, s.valueAt(t0 + 30_000))
        assertEquals(t0 + 60_000, s.nextZeroCrossing(t0))
        assertEquals(false, s.isAtRest)
        assertEquals(true, s.reset().isAtRest)
    }
}
