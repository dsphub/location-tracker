package com.dsp.androidsample.ui.service.location.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.w
import com.dsp.androidsample.ui.service.location.CustomLocationListener
import com.dsp.androidsample.ui.service.location.Event
import com.dsp.androidsample.ui.service.location.LocationEvent
import com.dsp.androidsample.ui.service.location.StateEvent
import java.util.*

class LocationManagerWrapper(private val context: Context) : CustomLocationListener() {
    private val locationManager: LocationManager = context
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            location?.let {
                subject.onNext(
                    LocationEvent(
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
            subject.onNext(
                StateEvent(
                    Date().time,
                    "provider=$provider status=${status}"
                )
            )
        }

        override fun onProviderEnabled(provider: String?) {
            subject.onNext(
                StateEvent(
                    Date().time,
                    "provider=$provider enabled"
                )
            )
        }

        override fun onProviderDisabled(provider: String?) {
            subject.onNext(
                StateEvent(
                    Date().time,
                    "provider=$provider disabled"
                )
            )
        }
    }

    override fun enable() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            w { "permission is not granted" }
            subject.onNext(
                StateEvent(
                    Date().time,
                    "Location permission is not granted"
                )
            )
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

    override fun getLastLocation(): Event {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            d { "getLastLocation gps" }
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).run {
                if (this == null) {
                    StateEvent(Date().time, "last location is unknown")
                } else {
                    LocationEvent(
                        provider,
                        time,
                        latitude,
                        longitude,
                        accuracy
                    )
                }
            }
        } else {
            throw IllegalStateException("location permission is not granted")
        }
    }

    companion object {
        const val MIN_TIME_MS = 0L
        const val MIN_DISTANCE_METER = 0F
    }
}