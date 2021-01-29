package com.dsp.androidsample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.w
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class LocationManagerWrapper(private val context: Context) {
    private var locationManager: LocationManager = context
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private lateinit var listener: LocationListener
    private val counter: AtomicInteger = AtomicInteger()
    private val subject = BehaviorSubject.create<String>()
    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
    }

    fun isGpsEnabled() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    fun enableGps() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun enable() {
        d { "enable" }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            w { "permission is not granted" }
            return
        }
        listener = object : LocationListenerAdapter() {
            override fun onLocationChanged(location: Location?) {
                val c = counter.incrementAndGet()
                d { "c=$c location=$location" }
                subject.onNext("$c: ${dateFormatter.format(Date())} lat=${location?.latitude} lon=${location?.longitude}")
            }
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

    fun disable() {
        d { "disable" }
        locationManager.removeUpdates(listener)
    }

    fun locationObservable(): Observable<String> = subject

    companion object {
        const val MIN_TIME_MS = 0L
        const val MIN_DISTANCE_METER = 0F
    }
}