package com.dsp.androidsample.data.events.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getEvents(): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY date ASC LIMIT (:limit) OFFSET (:offset)")
    fun getEventsByChunk(limit: Int, offset: Int): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id=(:id)")
    fun getEvent(id: Int): LiveData<EventEntity>

    @Update
    fun updateEvent(eventEntity: EventEntity)

    @Insert
    fun addEvent(eventEntity: EventEntity)

    @Insert
    fun addEvents(eventEntities: List<EventEntity>)

    @Query("DELETE FROM events")
    fun clean()
}