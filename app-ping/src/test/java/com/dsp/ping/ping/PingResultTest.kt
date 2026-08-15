package com.dsp.ping.ping

import com.dsp.ping.data.db.PingStatus
import com.dsp.ping.icons.IconStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PingResultTest {

    @Test
    fun `Ok toStatus returns PingStatus OK`() {
        assertEquals(PingStatus.OK, PingResult.Ok(123L).toStatus())
    }

    @Test
    fun `Fail toStatus returns PingStatus FAIL`() {
        assertEquals(PingStatus.FAIL, PingResult.Fail("timeout").toStatus())
    }

    @Test
    fun `NoNetwork toStatus returns PingStatus NO_NETWORK`() {
        assertEquals(PingStatus.NO_NETWORK, PingResult.NoNetwork.toStatus())
    }

    @Test
    fun `Ok toIconStatus is GREEN`() {
        assertEquals(IconStatus.GREEN, PingResult.Ok(1L).toIconStatus())
    }

    @Test
    fun `Fail toIconStatus is RED`() {
        assertEquals(IconStatus.RED, PingResult.Fail("err").toIconStatus())
    }

    @Test
    fun `NoNetwork toIconStatus is GRAY`() {
        assertEquals(IconStatus.GRAY, PingResult.NoNetwork.toIconStatus())
    }

    @Test
    fun `status strings match expected DB values`() {
        assertEquals("ok", PingStatus.OK)
        assertEquals("fail", PingStatus.FAIL)
        assertEquals("no_network", PingStatus.NO_NETWORK)
    }

    @Test
    fun `Ok holds latency`() {
        assertEquals(456L, PingResult.Ok(456L).latencyMs)
    }

    @Test
    fun `Fail holds error message`() {
        assertEquals("connection refused", PingResult.Fail("connection refused").error)
    }
}
