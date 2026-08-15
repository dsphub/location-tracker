package com.dsp.ping.data

import com.dsp.ping.notifications.AvailabilityCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvailabilityTest {

    @Test
    fun `3 ok + 1 fail = 75 percent`() {
        assertEquals(75, AvailabilityCalculator.percent(3, 1))
    }

    @Test
    fun `0 ok + 0 fail = null (no data)`() {
        assertNull(AvailabilityCalculator.percent(0, 0))
    }

    @Test
    fun `only no_network = null (no ok or fail counts)`() {
        // no_network записи не передаются в калькулятор: ok = 0, fail = 0
        assertNull(AvailabilityCalculator.percent(0, 0))
    }

    @Test
    fun `100 percent when no failures`() {
        assertEquals(100, AvailabilityCalculator.percent(5, 0))
    }

    @Test
    fun `0 percent when all failures`() {
        assertEquals(0, AvailabilityCalculator.percent(0, 5))
    }

    @Test
    fun `rounding down to integer percent`() {
        assertEquals(66, AvailabilityCalculator.percent(2, 1))
    }
}
