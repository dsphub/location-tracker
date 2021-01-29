package com.dsp.androidsample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.*
import android.telephony.CellSignalStrength.*
import androidx.annotation.RequiresApi
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
        d { "init" }
    }

    fun isGpsEnabled() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    private fun isNetworkEnabled() =
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    private fun isPassiveEnabled() =
        locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)

    fun enableGps() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun enable() {
        val providers = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.LOCATION_PROVIDERS_ALLOWED
        )
        d { "enable gps=${isGpsEnabled()} net=${isNetworkEnabled()} pas=${isPassiveEnabled()}" }
        d { "providers $providers" }
        signalStrength()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            w { "permission is not granted" }
            return
        }
        listener = object : LocationListenerAdapter() {
            override fun onLocationChanged(location: Location?) {
                val c = counter.incrementAndGet()
                d { "$c: ${dateFormatter.format(Date())} p=${location?.provider} lat=${location?.latitude} lon=${location?.longitude} acc=${location?.accuracy}" }
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

    private fun signalStrength() {
        wifiSignalStrength()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mobileSignalStrength()
    }

    private fun wifiSignalStrength() {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val rssi = wifiManager.connectionInfo.rssi
        val level = WifiManager.calculateSignalLevel(rssi, 4)
        d { "wifi enabled=${wifiManager.isWifiEnabled} rssi=$rssi level[0..4]=$level" }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mobileSignalStrength() {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.allCellInfo.isEmpty()) {
            d { "no active mobile cells" }
        }
        val cellSignalStrengthGsm: CellSignalStrength = when (telephonyManager.allCellInfo[0]) {
            is CellInfoGsm -> {
                (telephonyManager.allCellInfo[0] as CellInfoGsm).cellSignalStrength
            }
            is CellInfoWcdma -> {
                (telephonyManager.allCellInfo[0] as CellInfoWcdma).cellSignalStrength
            }
            is CellInfoLte -> {
                (telephonyManager.allCellInfo[0] as CellInfoLte).cellSignalStrength
            }
            else -> {
                w { "unknown cell type ${telephonyManager.allCellInfo[0]}" }
                return
            }
        }
        /*
         * Return the Received Signal Strength Indicator
         *
         * @return the RSSI in dBm (-113, -51) or
         *         {@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE}.
         */
        val enabled = telephonyManager.isDataEnabled
        val rssi = cellSignalStrengthGsm.dbm
        val asu = cellSignalStrengthGsm.asuLevel

        d { "mobile enabled=${enabled} rssi=$rssi level[0..4]=${getGsmLevel(asu)}" }
    }

    /**
     * Get gsm as level 0..4
     *
     * @hide
     */
    private fun getGsmLevel(asu: Int): Int {
        // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
        // asu = 0 (-113dB or less) is very weak
        // signal, its better to show 0 bars to the user in such cases.
        // asu = 99 is a special case, where the signal strength is unknown.
        return if (asu <= 2 || asu == 99) SIGNAL_STRENGTH_NONE_OR_UNKNOWN
        else if (asu >= 12) SIGNAL_STRENGTH_GREAT
        else if (asu >= 8) SIGNAL_STRENGTH_GOOD
        else if (asu >= 5) SIGNAL_STRENGTH_MODERATE
        else SIGNAL_STRENGTH_POOR
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