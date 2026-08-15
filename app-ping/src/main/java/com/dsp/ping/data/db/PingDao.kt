package com.dsp.ping.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PingDao {
    @Insert
    fun insert(ping: PingEntity): Long

    @Query("SELECT COUNT(*) FROM pings WHERE timestamp >= :since AND status = :status")
    fun countByStatusSince(since: Long, status: String): Int

    @Query("SELECT * FROM pings WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): List<PingEntity>

    @Query("DELETE FROM pings WHERE timestamp < :before")
    fun deleteOlderThan(before: Long): Int
}
