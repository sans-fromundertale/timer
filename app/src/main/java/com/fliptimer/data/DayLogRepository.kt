package com.fliptimer.data

import com.fliptimer.core.DayEnd
import com.fliptimer.core.TimerState
import com.fliptimer.core.localDate
import com.fliptimer.core.pendingDayEnds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class DayLogRepository(private val dao: DayEndDao, private val store: TimerStore) {
    /** Newest first. */
    val days: Flow<List<DayEnd>> = dao.observeAll().map { list ->
        list.map { DayEnd(LocalDate.ofEpochDay(it.epochDay), it.valueMillis) }
    }

    /** Records every day that ended since the last call. Must run before each timer change. */
    suspend fun catchUp(state: TimerState, now: Long, zone: ZoneId = ZoneId.systemDefault()) {
        val today = localDate(now, zone)
        val yesterday = today.minusDays(1)
        val last = store.lastLoggedEpochDay?.let(LocalDate::ofEpochDay)
        val pending = pendingDayEnds(state, last, today, zone)
        if (pending.isNotEmpty()) {
            dao.insertAll(pending.map { DayEndEntity(it.date.toEpochDay(), it.valueMillis) })
        }
        if (last == null || last < yesterday) store.lastLoggedEpochDay = yesterday.toEpochDay()
    }
}
