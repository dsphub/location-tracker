package com.dsp.ping.ping

import com.dsp.ping.data.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты чистой логики периода пинга: парсинг с границами и форматирование.
 * Границы берём из [SettingsStore], чтобы тесты ловили их изменение.
 */
class PingIntervalTest {

    private val min = SettingsStore.MIN_INTERVAL_SEC
    private val max = SettingsStore.MAX_INTERVAL_SEC

    //region parse

    @Test
    fun `parses plain number`() {
        assertEquals(30L, PingInterval.parse("30", min, max))
    }

    @Test
    fun `trims whitespace`() {
        assertEquals(60L, PingInterval.parse("  60  ", min, max))
    }

    @Test
    fun `accepts boundaries`() {
        assertEquals(1L, PingInterval.parse("1", min, max))
        assertEquals(86400L, PingInterval.parse("86400", min, max))
    }

    @Test
    fun `rejects value below min`() {
        assertNull(PingInterval.parse("0", min, max))
    }

    @Test
    fun `rejects value above max`() {
        assertNull(PingInterval.parse("86401", min, max))
    }

    @Test
    fun `rejects negative`() {
        assertNull(PingInterval.parse("-5", min, max))
    }

    @Test
    fun `rejects non-numeric input`() {
        assertNull(PingInterval.parse("abc", min, max))
        assertNull(PingInterval.parse("1.5", min, max))
        assertNull(PingInterval.parse("1 мин", min, max))
    }

    @Test
    fun `rejects empty and null`() {
        assertNull(PingInterval.parse("", min, max))
        assertNull(PingInterval.parse("   ", min, max))
        assertNull(PingInterval.parse(null, min, max))
    }

    @Test
    fun `rejects long overflow`() {
        assertNull(PingInterval.parse("99999999999999999999", min, max))
    }

    //endregion

    //region format

    @Test
    fun `formats seconds only`() {
        assertEquals("45 сек", PingInterval.format(45))
    }

    @Test
    fun `formats minutes only`() {
        assertEquals("1 мин", PingInterval.format(60))
        assertEquals("5 мин", PingInterval.format(300))
    }

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("1 мин 30 сек", PingInterval.format(90))
    }

    @Test
    fun `formats hours`() {
        assertEquals("1 ч", PingInterval.format(3600))
        assertEquals("2 ч 30 мин", PingInterval.format(9000))
    }

    @Test
    fun `formats full day`() {
        assertEquals("1 день", PingInterval.format(86400))
    }

    @Test
    fun `formats zero`() {
        assertEquals("0 сек", PingInterval.format(0))
    }

    //endregion
}
