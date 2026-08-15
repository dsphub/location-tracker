package com.dsp.ping.service

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dsp.ping.data.PingRepository
import com.dsp.ping.data.SettingsStore
import com.dsp.ping.data.db.PingEntity
import com.dsp.ping.data.network.NetworkMonitor
import com.dsp.ping.icons.IconStatus
import com.dsp.ping.icons.IconSwitcher
import com.dsp.ping.log.Logger.d
import com.dsp.ping.notifications.AvailabilityCalculator
import com.dsp.ping.notifications.PingNotificationManager
import com.dsp.ping.ping.HttpPinger
import com.dsp.ping.ping.PingResult
import com.dsp.ping.ping.toStatus
import com.dsp.ping.ping.toIconStatus
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

/**
 * Foreground-сервис периодического пинга (5 минут) с Doze-поддержкой через AlarmManager.
 */
class PingService : Service() {

    private val repository: PingRepository by inject()
    private val settingsStore: SettingsStore by inject()
    private val networkMonitor: NetworkMonitor by inject()
    private val pinger: HttpPinger by inject()
    private val notificationManager: PingNotificationManager by inject()
    private val iconSwitcher: IconSwitcher by inject()

    private val disposer = CompositeDisposable()
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var lastPingAt = 0L

    /**
     * Устанавливается в [shutdown]. Гарантирует, что идущий на io-потоке пинг не
     * перепланирует будильник и не перепостит нотификацию после остановки сервиса.
     */
    @Volatile
    private var isShutdown = false

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()

        val host = settingsStore.getHost() ?: getString(com.dsp.ping.R.string.app_name)
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notificationManager.build(host, null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        acquireWakeLock()
        disposer.add(
            Observable.interval(0, PING_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe { performPing() }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_PING -> disposer.add(
                Schedulers.io().scheduleDirect { performPing() }
            )

            ACTION_CLOSE -> shutdown()
            else -> {
                // ACTION_START / рестарт системы (null-intent): сервис уже работает.
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        disposer.dispose()
        releaseWakeLock()
        cancelAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun performPing() {
        if (isShutdown) return
        d { "=>performPing" }
        val now = System.currentTimeMillis()
        if (now - lastPingAt < MIN_PING_INTERVAL_MS) return
        lastPingAt = now

        val host = settingsStore.getHost() ?: return

        val result = if (networkMonitor.isOnline()) {
            pinger.ping(host)
        } else {
            PingResult.NoNetwork
        }

        val entity = when (result) {
            is PingResult.Ok -> PingEntity(
                timestamp = now,
                host = host,
                status = result.toStatus(),
                latencyMs = result.latencyMs
            )

            is PingResult.Fail -> PingEntity(
                timestamp = now,
                host = host,
                status = result.toStatus(),
                error = result.error
            )

            PingResult.NoNetwork -> PingEntity(
                timestamp = now,
                host = host,
                status = result.toStatus()
            )
        }
        d { "performPing $entity" }
        repository.addResult(entity)
        iconSwitcher.apply(result.toIconStatus())

        repository.requestAvailability24h { availability ->
            updateNotification(
                host,
                AvailabilityCalculator.percent(availability.ok, availability.fail)
            )
        }

        if (!isShutdown) {
            scheduleNextAlarm()
        }
    }

    private fun updateNotification(host: String, percent: Int?) {
        if (isShutdown) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(this)
            .notify(NOTIF_ID, notificationManager.build(host, percent))
    }

    private fun shutdown() {
        isShutdown = true
        disposer.dispose()
        cancelAlarm()
        releaseWakeLock()
        iconSwitcher.apply(IconStatus.GRAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()

        // appTasks появился в API 23; на 21-22 просто пропускаем очистку recents.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.appTasks.forEach { it.finishAndRemoveTask() }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
        }, KILL_DELAY_MS)
    }

    //region wake lock

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$packageName:pingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    //endregion

    //region doze alarm

    private fun scheduleNextAlarm() {
        if (isShutdown) return
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent()
        val triggerAt = System.currentTimeMillis() + PING_INTERVAL_MS

        val exact = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms()

        when {
            exact -> alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
            )

            else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, PingAlarmReceiver::class.java).setAction(ACTION_PING),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    //endregion

    companion object {
        const val ACTION_START = "com.dsp.ping.action.START"
        const val ACTION_PING = "com.dsp.ping.action.PING"
        const val ACTION_CLOSE = "com.dsp.ping.action.CLOSE"
        const val EXTRA_FROM_NOTIFICATION = "com.dsp.ping.extra.FROM_NOTIFICATION"
        const val NOTIF_ID = 1001
        const val PING_INTERVAL_MS = 5 * 60_000L

        private const val MIN_PING_INTERVAL_MS = 4 * 60_000L
        private const val KILL_DELAY_MS = 300L
    }
}
