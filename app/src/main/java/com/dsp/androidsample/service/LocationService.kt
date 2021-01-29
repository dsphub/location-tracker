package com.dsp.androidsample.service

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dsp.androidsample.CustomNotificationManager
import com.dsp.androidsample.LocationManagerWrapper
import com.dsp.androidsample.SimpleNotification
import com.dsp.androidsample.add
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.e
import com.dsp.androidsample.log.Logger.i
import com.dsp.androidsample.ui.MainActivity
import io.reactivex.disposables.CompositeDisposable


class LocationService : Service() {
    private val locationManager by lazy { LocationManagerWrapper(this) }
    private val notificationManager by lazy { CustomNotificationManager(this) }
    private val disposer = CompositeDisposable()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stop: Boolean = intent?.getBooleanExtra(
            EXTRA_ACTION_STOP,
            false
        ) ?: false
        i { "onCreate stop=$stop" }
        if (stop) {
            locationManager.disable()
            stopSelf()
            // Tells the system to not try to recreate the service after it has been killed
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        i { "onCreate" }
        super.onCreate()
        notificationManager.makeChannel()
        val n = createNotification("Started").build()
        startForeground(FG_LOCATION_NID, n)
        locationManager.enable()

        disposer.add = locationManager.locationObservable()
            .subscribe({
                d { "DBG $it" }
                showNotification(it)
            }, {
                e { "DBG failed: ${it.message}" }
                showNotification(it.message ?: "No error")
            })
    }

    private fun showNotification(text: String) {
        if (serviceIsRunningInForeground(this)) {
            val n = SimpleNotification(
                FG_LOCATION_NID,
                createNotification(text)
            )
            notificationManager.show(n)
        } else {
            val n = createNotification(text).build()
            startForeground(FG_LOCATION_NID, n)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        i { "onBind" }
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        val service: LocationService
            get() = this@LocationService
    }

    private fun createNotification(location: String) =
        NotificationCompat.Builder(this)
            .addAction(android.R.drawable.ic_dialog_map, "Stop", servicePendingIntent())
            .setChannelId(CustomNotificationManager.CHANNEL_ID) //FIXIT for Oreo+ only
            .setContentText(location)
            .setContentTitle("Fg Location")
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setWhen(System.currentTimeMillis())

    // The PendingIntent that leads to a call to onStartCommand() in this service.
    private fun servicePendingIntent() = PendingIntent.getService(
        this, 0,
        Intent(this, LocationService::class.java).apply {
            putExtra(EXTRA_ACTION_STOP, true)
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun activityPendingIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java), 0
    )

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The [Context].
     */
    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private const val FG_LOCATION_NID = 2
        const val EXTRA_ACTION_STOP = "started_from_notification"
        const val channelId = "fg_channel"
    }
}