package com.dsp.androidsample.data.events

import android.content.Context
import androidx.lifecycle.LiveData
import com.dsp.androidsample.data.events.db.EventDatabase
import com.dsp.androidsample.data.events.db.EventEntity
import com.dsp.androidsample.log.Logger.d
import java.util.concurrent.ExecutorService

class EventRepository private constructor(
    private val db: EventDatabase,
    private val executor: ExecutorService
) {
    private val eventDao = db.eventDao()

    fun getEvents(): LiveData<List<EventEntity>> = eventDao.getEvents()

    fun getEvent(id: Int): LiveData<EventEntity> = eventDao.getEvent(id)

    fun updateEvent(myLocationEntity: EventEntity) {
        executor.execute {
            eventDao.updateEvent(myLocationEntity)
        }
    }

    fun addEvent(event: EventEntity) {
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