package com.dsp.ping.data

import android.os.Handler
import android.os.Looper
import com.dsp.ping.data.db.PingDao
import com.dsp.ping.data.db.PingEntity
import com.dsp.ping.data.db.PingStatus
import java.util.concurrent.Executor

class PingRepository(
    private val dao: PingDao,
    private val diskIo: Executor
) {

    data class Availability(val ok: Int, val fail: Int)

    private val mainThread = Handler(Looper.getMainLooper())
    private var insertCount = 0

    fun addResult(ping: PingEntity) {
        diskIo.execute {
            dao.insert(ping)
            insertCount++
            if (insertCount % PURGE_EVERY == 0) {
                purgeOlderThan48h()
            }
        }
    }

    fun requestAvailability24h(cb: (Availability) -> Unit) {
        val since = System.currentTimeMillis() - WINDOW_24H_MS
        diskIo.execute {
            val ok = dao.countByStatusSince(since, PingStatus.OK)
            val fail = dao.countByStatusSince(since, PingStatus.FAIL)
            val result = Availability(ok, fail)
            mainThread.post { cb(result) }
        }
    }

    fun requestRecent(since: Long, cb: (List<PingEntity>) -> Unit) {
        diskIo.execute {
            val result = dao.getSince(since)
            mainThread.post { cb(result) }
        }
    }

    fun purgeOlderThan48h() {
        val before = System.currentTimeMillis() - WINDOW_48H_MS
        diskIo.execute { dao.deleteOlderThan(before) }
    }

    private companion object {
        const val WINDOW_24H_MS = 24L * 60L * 60L * 1000L
        const val WINDOW_48H_MS = 48L * 60L * 60L * 1000L
        const val PURGE_EVERY = 12
    }
}
