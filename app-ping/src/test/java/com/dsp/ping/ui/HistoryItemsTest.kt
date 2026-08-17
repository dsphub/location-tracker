package com.dsp.ping.ui

import com.dsp.ping.data.db.PingEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/**
 * Тесты группировки истории по дням ([buildHistoryItems]) и форматирования
 * заголовков-дат ([HistoryDateFormatter]). Локаль зафиксирована, чтобы тесты
 * не зависели от окружения.
 */
class HistoryItemsTest {

    private val locale = Locale.US

    //region buildHistoryItems

    @Test
    fun `empty list yields no items`() {
        assertEquals(
            emptyList<HistoryItem>(),
            buildHistoryItems(emptyList(), dayKey = { "" }, dayTitle = { "" })
        )
    }

    @Test
    fun `single day gets one header before first ping`() {
        val pings = listOf(ping(1, ts(2026, Calendar.SEPTEMBER, 1, 12, 0)))
        val items = buildHistoryItems(
            pings,
            dayKey = { HistoryDateFormatter.dayKey(it, locale) },
            dayTitle = { HistoryDateFormatter.dayTitle(it, locale) }
        )
        assertEquals(2, items.size)
        assertEquals(
            HistoryItem.DateHeader(ts(2026, Calendar.SEPTEMBER, 1, 12, 0), "1 September 2026"),
            items[0]
        )
        assertEquals(HistoryItem.PingRow(pings[0]), items[1])
    }

    @Test
    fun `header inserted between days`() {
        val pings = listOf(
            ping(1, ts(2026, Calendar.SEPTEMBER, 2, 10, 0)),
            ping(2, ts(2026, Calendar.SEPTEMBER, 1, 23, 59)),
            ping(3, ts(2026, Calendar.SEPTEMBER, 1, 12, 0)),
            ping(4, ts(2026, Calendar.AUGUST, 31, 23, 0))
        )
        val items = buildHistoryItems(
            pings,
            dayKey = { HistoryDateFormatter.dayKey(it, locale) },
            dayTitle = { HistoryDateFormatter.dayTitle(it, locale) }
        )
        // 4 пинга + 3 заголовка дней
        assertEquals(7, items.size)
        assertEquals("2 September 2026", (items[0] as HistoryItem.DateHeader).title)
        assertEquals("1 September 2026", (items[2] as HistoryItem.DateHeader).title)
        assertEquals("31 August 2026", (items[5] as HistoryItem.DateHeader).title)
    }

    @Test
    fun `midnight boundary separates days`() {
        val pings = listOf(
            ping(1, ts(2026, Calendar.SEPTEMBER, 2, 0, 0)),
            ping(2, ts(2026, Calendar.SEPTEMBER, 1, 23, 59, 59))
        )
        val items = buildHistoryItems(
            pings,
            dayKey = { HistoryDateFormatter.dayKey(it, locale) },
            dayTitle = { HistoryDateFormatter.dayTitle(it, locale) }
        )
        assertEquals(4, items.size)
    }

    //endregion

    //region HistoryDateFormatter

    @Test
    fun `day title format day month year`() {
        assertEquals(
            "1 September 2026",
            HistoryDateFormatter.dayTitle(ts(2026, Calendar.SEPTEMBER, 1, 15, 30), locale)
        )
    }

    @Test
    fun `day key is zero padded`() {
        assertEquals(
            "2026-09-01",
            HistoryDateFormatter.dayKey(ts(2026, Calendar.SEPTEMBER, 1, 15, 30), locale)
        )
    }

    @Test
    fun `day key same within day and different across days`() {
        val morning = HistoryDateFormatter.dayKey(ts(2026, Calendar.SEPTEMBER, 1, 6, 0), locale)
        val evening = HistoryDateFormatter.dayKey(ts(2026, Calendar.SEPTEMBER, 1, 23, 0), locale)
        val nextDay = HistoryDateFormatter.dayKey(ts(2026, Calendar.SEPTEMBER, 2, 0, 0), locale)
        assertEquals(morning, evening)
        org.junit.Assert.assertNotEquals(morning, nextDay)
    }

    //endregion

    private fun ping(id: Long, timestamp: Long) = PingEntity(
        id = id,
        timestamp = timestamp,
        host = "https://example.com",
        status = "ok",
        latencyMs = 100L
    )

    private fun ts(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long =
        Calendar.getInstance(locale).apply {
            clear()
            set(year, month, day, hour, minute, second)
        }.timeInMillis
}
