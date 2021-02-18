package com.dsp.androidsample.ui.service.location

import android.content.Context
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.ui.service.SystemServiceFacade
import com.dsp.androidsample.ui.service.location.core.LocationManagerWrapper
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.text.SimpleDateFormat
import java.util.*

class LocationManagerFacade(private val context: Context) {
    private val locationManager: CustomLocationListener by lazy { LocationManagerWrapper(context) }
    private val serviceFacade = SystemServiceFacade(context)
    private val subject = BehaviorSubject.create<Event>()
    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    val events: Observable<Event> = locationManager.events

//    fun locationObservable(): Observable<Event> = subject

    init {
        d { "init" }
    }

    fun enable() {
        d { "enable" }
        serviceFacade.logDataSaverPrefs()
        serviceFacade.logLocationProviders()
        serviceFacade.logSignalStrength()
        serviceFacade.logScreenModes()
/*
        locationManager.events.subscribe(
            {
                logEvent(it)
            },
            {
                e { "subscribe location failed: $it" }
            })
*/
        locationManager.enable()
    }

    private fun logEvent(event: Event) {
        when (event) {
            is LocationEvent -> {
                d { "location: ${dateFormatter.format(Date())} p=${event.provider} lat=${event.latitude} lon=${event.longitude} acc=${event.accuracy}" }
                subject.onNext(event)
            }
            is StateEvent -> {
                d { "state: ${dateFormatter.format(Date())} ${event.state}" }
                subject.onNext(event)
            }
        }
    }

    fun getLastLocation(): Event = locationManager.getLastLocation()

    fun disable() {
        d { "disable" }
        locationManager.disable()
    }
}