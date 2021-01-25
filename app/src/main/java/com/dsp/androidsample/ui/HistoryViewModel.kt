package com.dsp.androidsample.ui

import android.app.Application
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dsp.androidsample.CustomNotificationManager
import com.dsp.androidsample.LocationManagerWrapper
import com.dsp.androidsample.SimpleNotification
import com.dsp.androidsample.add
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.e
import com.dsp.androidsample.log.Logger.i
import io.reactivex.disposables.CompositeDisposable

class HistoryViewModel(private val app: Application) : AndroidViewModel(app) {
    private val locationManager by lazy { LocationManagerWrapper(app.baseContext) }
    private val notificationManager by lazy { CustomNotificationManager(app.baseContext) }

    private val _location = MutableLiveData<String>()
    val location: LiveData<String>
        get() = _location

    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    private val disposer = CompositeDisposable()

    init {
        d { "init" }
        if (!locationManager.isGpsEnabled()) {
            locationManager.enableGps()
        }
        locationManager.enable()
        disposer.add = locationManager.locationObservable()
            .subscribe({
                _location.value = it
                d { "DBG ${it}" }
                val n = SimpleNotification(
                    LOCATION_NID,
                    createNotification(it)
                )
                notificationManager.show(n)
            }, {
                e { "DBG failed: ${it.message}" }
                _error.value = it.message
                val n = SimpleNotification(
                    LOCATION_NID,
                    createNotification(it.message ?: "No error")
                )
                notificationManager.show(n)
            })
    }

    private fun createNotification(location: String) =
        NotificationCompat.Builder(app.baseContext)
            .setChannelId(CustomNotificationManager.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Location")
            .setContentText(location)

    override fun onCleared() {
        i { "onCleared" }
        locationManager.disable()
        super.onCleared()
        notificationManager.hide(LOCATION_NID)
    }

    companion object {
        private const val LOCATION_NID = 1
    }
}