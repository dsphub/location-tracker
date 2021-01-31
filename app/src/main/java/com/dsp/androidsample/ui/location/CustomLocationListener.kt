package com.dsp.androidsample.ui.location

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

interface Event

data class LocationEvent(
    val id: Int,
    val provider: String,
    val time: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
) : Event

data class StateEvent(
    val id: Int,
    val time: Long,
    val state: String
) : Event

data class ErrorEvent(
    val message: String
) : Event

abstract class CustomLocationListener {
    protected val subject: BehaviorSubject<Event> = BehaviorSubject.create<Event>()
    val events: Observable<Event> = subject

    abstract fun enable()
    abstract fun disable()
}
