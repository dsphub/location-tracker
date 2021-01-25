package com.dsp.androidsample

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import com.dsp.androidsample.log.Logger.d

abstract class LocationListenerAdapter : LocationListener {
    override fun onLocationChanged(location: Location?) {
        d { "location=$location" }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        d { "changed $provider status=$status" }
    }

    override fun onProviderEnabled(provider: String?) {
        d { "enabled $provider" }
    }

    override fun onProviderDisabled(provider: String?) {
        d { "disabled $provider" }
    }
}