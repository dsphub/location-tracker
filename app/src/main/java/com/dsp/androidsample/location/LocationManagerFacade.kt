package com.dsp.androidsample.location

import android.content.Context
import com.dsp.androidsample.location.core.LocationManagerWrapper
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.e
import com.dsp.androidsample.service.SystemServiceFacade
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.text.SimpleDateFormat
import java.util.*

class LocationManagerFacade(private val context: Context) {
    private val locationManager: CustomLocationListener by lazy { LocationManagerWrapper(context) }
    private val serviceFacade = SystemServiceFacade(context)
    private val subject = BehaviorSubject.create<String>()
    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun locationObservable(): Observable<String> = subject

    init {
        d { "init" }
    }

    fun enable() {
        d { "enable" }
        serviceFacade.logLocationProviders()
        serviceFacade.logSignalStrength()
        serviceFacade.logScreenModes()
        locationManager.events.subscribe(
            {
                logEvent(it)
            },
            {
                e { "subscribe location failed: $it" }
            })
        locationManager.enable()
    }

    private fun logEvent(event: Event) {
        when (event) {
            is LocationEvent -> {
                d { "location: ${event.id}: ${dateFormatter.format(Date())} p=${event.provider} lat=${event.latitude} lon=${event.longitude} acc=${event.accuracy}" }
                val str =
                    "${event.id}: ${dateFormatter.format(Date())} p=${event.provider} lat=${event.latitude} lon=${event.longitude}"
                subject.onNext(str)
            }
            is StateEvent -> d { "state: ${event.state}" }
        }
    }

    fun disable() {
        d { "disable" }
        locationManager.disable()
    }
}