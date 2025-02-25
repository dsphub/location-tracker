package com.dsp.androidsample.data.events

import android.content.Context
import androidx.lifecycle.LiveData
import com.dsp.androidsample.data.events.db.EventDatabase
import com.dsp.androidsample.data.events.db.EventEntity
import com.dsp.androidsample.log.Logger.d
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

class EventRepository private constructor(
    private val db: EventDatabase,
    private val executor: ExecutorService
) {
    private val eventDao = db.eventDao()
    private val counter: AtomicInteger = AtomicInteger()

    fun getEvents(): LiveData<List<EventEntity>> = eventDao.getEvents()

    fun getEventsByChunk(limit: Int, offset: Int): LiveData<List<EventEntity>> =
        eventDao.getEventsByChunk(limit, offset)

    fun getEvent(id: Int): LiveData<EventEntity> = eventDao.getEvent(id)

    fun updateEvent(myLocationEntity: EventEntity) {
        executor.execute {
            eventDao.updateEvent(myLocationEntity)
        }
    }

    fun addEvent(event: EventEntity) {
        if (event.id == 0) {
            event.id = counter.incrementAndGet()
        }
        executor.execute {
            d { "$event" }
            eventDao.addEvent(event)
        }
    }

    fun addEvents(events: List<EventEntity>) {
        executor.execute {
            eventDao.addEvents(events)
        }
    }

    fun clean() {
        executor.execute {
            eventDao.clean()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): EventRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository(
                    EventDatabase.getInstance(context), executor
                )
                    .also { INSTANCE = it }
            }
        }
    }
}