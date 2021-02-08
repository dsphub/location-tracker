package com.dsp.androidsample.ui.service.location.fused

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.w
import com.dsp.androidsample.ui.service.location.CustomLocationListener
import com.dsp.androidsample.ui.service.location.LocationEvent
import com.dsp.androidsample.ui.service.location.StateEvent
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.location.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FusedLocationProviderWrapper(val context: Context) : CustomLocationListener() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var request: LocationRequest
    private var callback: LocationCallback
    private val counter: AtomicInteger = AtomicInteger()

    init {
        request = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(0)
            fastestInterval = TimeUnit.SECONDS.toMillis(0)
            maxWaitTime = TimeUnit.MINUTES.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result?.let {
                    d { "locations=${it.locations.size}" }
                    subject.onNext(mapToEvent(result))
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability?) {
                availability?.let {
                    subject.onNext(
                        StateEvent(
                            Date().time,
                            "location availability=$availability"
                        )
                    )
                }
            }
        }
    }

    private fun mapToEvent(result: LocationResult): LocationEvent {
        return LocationEvent(
            result.lastLocation.provider,
            result.lastLocation.time,
            result.lastLocation.latitude,
            result.lastLocation.longitude,
            result.lastLocation.accuracy
        )
    }

    @Throws(SecurityException::class)
    @MainThread
    override fun enable() {
        d { "enable" }
        val gpa = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)
        if (gpa != ConnectionResult.SUCCESS) {
            w { "GooglePlayService is not available" }
            subject.onNext(
                StateEvent(
                    Date().time,
                    "GooglePlayService is not available"
                )
            )
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (permissionRevoked: SecurityException) {
            subject.onNext(
                StateEvent(
                    Date().time,
                    "location permission revoked"
                )
            )
        }
    }

    override fun disable() {
        d { "disable" }
        fusedLocationClient.removeLocationUpdates(callback)
    }
}