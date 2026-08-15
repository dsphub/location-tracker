package com.dsp.ping.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object PingStatus {
    const val OK = "ok"
    const val FAIL = "fail"
    const val NO_NETWORK = "no_network"
}

@Entity(tableName = "pings")
data class PingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val host: String,
    val status: String,
    val latencyMs: Long? = null,
    val error: String? = null
)
