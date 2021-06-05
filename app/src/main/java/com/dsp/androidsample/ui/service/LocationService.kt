package com.dsp.androidsample.ui.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.GnssStatus
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.dsp.androidsample.BuildConfig
import com.dsp.androidsample.CustomNotificationManager
import com.dsp.androidsample.SimpleNotification
import com.dsp.androidsample.add
import com.dsp.androidsample.data.events.EventRepository
import com.dsp.androidsample.data.events.db.EventEntity
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.e
import com.dsp.androidsample.log.Logger.i
import com.dsp.androidsample.ui.MainActivity
import com.dsp.androidsample.ui.service.location.Event
import com.dsp.androidsample.ui.service.location.LocationEvent
import com.dsp.androidsample.ui.service.location.LocationManagerFacade
import com.dsp.androidsample.ui.service.location.StateEvent
import com.dsp.androidsample.ui.service.location.doze.AlarmBroadcastReceiver
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LocationService : Service() {
    private val locationManager by lazy { LocationManagerFacade(this) }
    private val notificationManager by lazy { CustomNotificationManager(this) }
    private val powerManager
        get() = (this.getSystemService(Context.POWER_SERVICE) as PowerManager)
    private val wifiManager
        get() = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val serviceFacade by lazy { SystemServiceFacade(this) }

    private val disposer = CompositeDisposable()

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private lateinit var idleReceiver: BroadcastReceiver
    private val eventRepository by lazy {
        EventRepository.getInstance(
            this,
            Executors.newSingleThreadExecutor()
        )
    }

    private val gpsListener: GnssStatus.Callback? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus?) {
                    setState("GPS satellites=${status?.satelliteCount}")
                }

                override fun onStarted() {
                    setState("GPS started")
                }

                override fun onFirstFix(ttffMillis: Int) {
                    setState("GPS ttffMs=$ttffMillis")
                }

                override fun onStopped() {
                    setState("GPS stopped")
                }
            }
        } else {
            null
        }
    }

    private val networkListener: ConnectivityManager.NetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                setState("network blocked=$blocked $network")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                setState("network capabilities changed $network $networkCapabilities")
            }

            override fun onLost(network: Network) {
                setState("network lost $network")
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                setState("network link properties changed $network")
            }

            override fun onUnavailable() {
                setState("network unavailable")
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                setState("network losing=$maxMsToLive $network")
            }

            override fun onAvailable(network: Network) {
                setState("network available $network")
            }
        }
    }

    fun setState(value: String) {
//DBG        d { "setState: $value" }
        eventRepository.addEvent(EventEntity(0, Date(), value))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stop: Boolean = intent?.getBooleanExtra(
            EXTRA_ACTION_STOP,
            false
        ) ?: false
        val wakeup: Boolean = intent?.getBooleanExtra(
            EXTRA_ACTION_WAKEUP,
            false
        ) ?: false
        i { "onStartCommand stop=$stop" }
        if (stop) {
            locationManager.disable()
            disposer.dispose()
            unregisterReceiver(idleReceiver)
            stopSelf()
            setState("service is stopped")
            // Tells the system to not try to recreate the service after it has been killed
            return START_NOT_STICKY
        }
        if (wakeup) {
            setState("alarmLocation: ${locationManager.getLastLocation()}")
            setupExactAlarm()
        }
        startWakeLock()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "${packageName}:mainWakeLock"
            )
            wakeLock?.acquire()
        }
        if (wifiManager.isWifiEnabled) {
            if (wifiLock == null) {
                wifiLock = wifiManager.createWifiLock("${packageName}:mainWifiLock")
                wifiLock?.setReferenceCounted(false)
                wifiLock?.acquire()
            }
        }
        d { "startWakeLock ${wakeLock?.isHeld} ${wifiLock?.isHeld}" }
    }

    override fun onDestroy() {
        d { "onDestroy" }
        stopWakeLock()
        eventRepository.clean()
        stopLogStates()
        super.onDestroy()
    }

    private fun stopWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
        }
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
            wifiLock = null
        }
    }

    private fun stopLogStates() {
        serviceFacade.unregisterNetworkCallback(networkListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gpsListener?.let {
                serviceFacade.unregisterGnssStatusCallback(it)
            }
        }
    }

    //region wakeup
    private var lastLocationDisposable: Disposable? = null

    private fun lastLocationOnIdle(): Observable<Event> {
        return Observable.interval(LAST_LOCATION_ON_IDLE_RETRY_SEC, TimeUnit.SECONDS)
            .map {
                locationManager.getLastLocation()
            }
    }

    private val handler by lazy { Handler() }
    private fun setupExactAlarm() {
        d { "DBG setupExactAlarm" }
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val task = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                d { "DBG setupExactAlarm and run" }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    ALARM_TRIGGER_MS,
                    pendingIntent
                )
            }
        }
        handler.postDelayed(task, 0)
    }
    //endregion

    override fun onCreate() {
        i { "onCreate version=${BuildConfig.VERSION_NAME}" }
        super.onCreate()
        eventRepository.clean()
        notificationManager.makeChannel()
        val n = createNotification("Started").build()
        startForeground(FG_LOCATION_NID, n)
        startLogStates()
        disposer.add = locationManager.events
            .subscribe({ event ->
                handleEvent(event)
            }, {
                e { "failed: ${it.message}" }
                showNotification(it.message ?: "No error")
            })
        locationManager.enable()
        registerIdleReceiver()
        setState("service is started")
//        wakeup()
    }

    private fun startLogStates() {
        serviceFacade.registerNetworkCallback(networkListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gpsListener?.let {
                serviceFacade.registerGnssStatusCallback(it)
            }
        }
    }

    private fun handleEvent(event: Event) {
        val entity = when (event) {
            is LocationEvent -> {
                mapTo(event)
            }
            is StateEvent -> {
                mapTo(event)
            }
            else -> {
                e { "Unknown event type $event" }
                return
            }
        }
        eventRepository.addEvent(entity)
        showNotification(entity.toString())
    }

    private fun mapTo(event: LocationEvent): EventEntity {
        return EventEntity(
            0,
            Date(event.time),
            "p=${event.provider} lat=${event.latitude} lon=${event.longitude} acc=${event.accuracy}"
        )
    }

    private fun mapTo(event: StateEvent): EventEntity {
        return EventEntity(
            0,
            Date(event.time),
            "state: ${event.state}"
        )
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

    private fun registerIdleReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val filter = IntentFilter().apply {
                addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            d { "registerIdleReceiver" }
            idleReceiver = DeviceIdleReceiver()
            disposer.add = (idleReceiver as DeviceIdleReceiver).observable
                .subscribe({ (saveMode, idleMode) ->
                    setState("PowerManager STATE: saveMode=$saveMode, idleMode=$idleMode")
                    if (idleMode) {
                        lastLocationDisposable?.dispose()
                        lastLocationDisposable = lastLocationOnIdle()
                            .subscribe {
                                setState("lastLocation: $it")
                            }
                        setupExactAlarm()
                    } else {
                        lastLocationDisposable?.dispose()
                        lastLocationDisposable = null
                    }
                }, {
                    e { "failed registerIdleReceiver: ${it.message}" }
                    setState("failed registerIdleReceiver: ${it.message}")
                })
            registerReceiver(idleReceiver, filter)
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
            .setContentIntent(activityPendingIntent())
            .setContentText(location)
            .setContentTitle("Location from foreground service")
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
    private fun serviceIsRunningInForeground(context: Context): Boolean {
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
        const val EXTRA_ACTION_WAKEUP = "wakeup_on_idle"
        private const val LAST_LOCATION_ON_IDLE_RETRY_SEC = 10L

        // https://developer.android.com/training/monitoring-device-state/doze-standby#assessing_your_app
        private const val ALARM_TRIGGER_MS = 1 * 60 * 1000L
    }
}