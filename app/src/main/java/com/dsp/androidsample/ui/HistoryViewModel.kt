package com.dsp.androidsample.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dsp.androidsample.data.events.EventRepository
import com.dsp.androidsample.data.events.db.EventEntity
import com.dsp.androidsample.isLocationPermissionGranted
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.service.SystemServiceFacade
import java.util.*

class HistoryViewModel(
    private val app: Application,
    private val eventRepository: EventRepository
) : AndroidViewModel(app) {
    private val serviceFacade by lazy { SystemServiceFacade(app.baseContext) }

    val events = eventRepository.getEvents()

    private val _location = MutableLiveData<String>()
    val location: LiveData<String>
        get() = _location

    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    init {
        d { "init gps=${serviceFacade.isGpsEnabled()}" }
        if (!serviceFacade.isGpsEnabled()) {
            serviceFacade.enableGps()
        }
        if (!app.isLocationPermissionGranted()) {
            eventRepository.clean()
        }
    }

    fun setState(value: String) {
        d { "setState: $value" }
        eventRepository.addEvent(EventEntity(0, Date(), value))
    }
}