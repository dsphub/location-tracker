package com.dsp.androidsample.location.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.dsp.androidsample.location.CustomLocationListener
import com.dsp.androidsample.location.LocationEvent
import com.dsp.androidsample.location.StateEvent
import com.dsp.androidsample.log.Logger.w
import java.util.concurrent.atomic.AtomicInteger

class LocationManagerWrapper(private val context: Context) : CustomLocationListener() {
    private val locationManager: LocationManager = context
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val counter: AtomicInteger = AtomicInteger()

    private val listener = object : LocationListenerAdapter() {
        override fun onLocationChanged(location: Location?) {
            super.onLocationChanged(location)
            location?.let {
                subject.onNext(
                    LocationEvent(
                        counter.incrementAndGet(),
                        it.provider,
                        it.time,
                        it.latitude,
                        it.longitude,
                        it.accuracy
                    )
                )
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            super.onStatusChanged(provider, status, extras)
            subject.onNext(StateEvent("provider=$provider status=${status}"))
        }

        override fun onProviderEnabled(provider: String?) {
            super.onProviderEnabled(provider)
            subject.onNext(StateEvent("provider=$provider enabled"))
        }

        override fun onProviderDisabled(provider: String?) {
            super.onProviderDisabled(provider)
            subject.onNext(StateEvent("provider=$provider disabled"))
        }
    }

    override fun enable() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            w { "permission is not granted" }
            subject.onNext(StateEvent("Location permission is not granted"))
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_TIME_MS,
            MIN_DISTANCE_METER,
            listener
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            MIN_TIME_MS,
            MIN_DISTANCE_METER,
            listener
        )
        locationManager.requestLocationUpdates(
            LocationManager.PASSIVE_PROVIDER,
            MIN_TIME_MS,
            MIN_DISTANCE_METER,
            listener
        )
    }

    override fun disable() {
        locationManager.removeUpdates(listener)
    }

    companion object {
        const val MIN_TIME_MS = 0L
        const val MIN_DISTANCE_METER = 0F
    }
}