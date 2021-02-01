package com.dsp.androidsample.data.events.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey var id: Int,
    val date: Date = Date(),
    val value: String = ""
) {
    override fun toString(): String {
        return "$id: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(date)} $value"
    }
}