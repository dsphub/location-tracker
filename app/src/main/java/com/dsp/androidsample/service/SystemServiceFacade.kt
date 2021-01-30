package com.dsp.androidsample.service

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.*
import androidx.annotation.RequiresApi
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.w

class SystemServiceFacade(private val context: Context) {
    private val locationManager: LocationManager = context
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val powerService = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    val isWifiEnabled = wifiManager.isWifiEnabled
    val wifiRssi = wifiManager.connectionInfo.rssi
    val wifiLevel = WifiManager.calculateSignalLevel(wifiRssi, 4)

    fun logSignalStrength() {
        logWifiSignalStrength()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            logMobileSignalStrength()
    }

    private fun logWifiSignalStrength() {
        d { "wifi enabled=${wifiManager.isWifiEnabled} rssi=$wifiRssi level[0..4]=$wifiLevel" }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun logMobileSignalStrength() {
        if (telephonyManager.allCellInfo.isEmpty()) {
            w { "no active mobile cells" }
            return
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
        return if (asu <= 2 || asu == 99) CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN
        else if (asu >= 12) CellSignalStrength.SIGNAL_STRENGTH_GREAT
        else if (asu >= 8) CellSignalStrength.SIGNAL_STRENGTH_GOOD
        else if (asu >= 5) CellSignalStrength.SIGNAL_STRENGTH_MODERATE
        else CellSignalStrength.SIGNAL_STRENGTH_POOR
    }

    fun logScreenModes() {
        d { "screenOn=${powerService.isInteractive}" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            d { "locationPowerSaveMode=${powerService.locationPowerSaveMode}" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            d { "idleMode=${powerService.isDeviceIdleMode}" }
        d { "powerSaveMode=${powerService.isPowerSaveMode}" }
    }

    fun logLocationProviders() {
        val providers = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.LOCATION_PROVIDERS_ALLOWED
        )
        d { "location providers $providers: gps=${isGpsEnabled()} net=${isNetworkEnabled()} pas=${isPassiveEnabled()}" }
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
}