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
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

/**
 * Foreground-сервис периодического пинга с Doze-поддержкой через AlarmManager.
 *
 * Расписание: пинг выполняется сразу при старте сервиса, далее — на каждой
 * границе wall-clock, кратной периоду из [SettingsStore.getIntervalSec]
 * (по умолчанию 1 минута). Смена периода в Settings подхватывается на лету:
 * Rx-таймер перезапускается, будильники и слоты дедупликации берут новый период
 * со следующего пинга. Оба планировщика (Rx-таймер и будильники) выравниваются
 * по одному и тому же правилу [nextBoundaryTime].
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

    /** Rx-таймер регулярных пингов; пересоздаётся при смене периода в Settings. */
    private var periodicTimer: Disposable? = null

    @Volatile
    private var lastPingAt = 0L

    /** Слот границы последнего выполненного пинга; читается/пишется только под [synchronized] на this. */
    private var lastPingSlot = -1L

    /**
     * Устанавливается в [shutdown]. Гарантирует, что идущий на io-потоке пинг не
     * перепланирует будильник и не перепостит нотификацию после остановки сервиса.
     */
    @Volatile
    private var isShutdown = false

    override fun onCreate() {
        super.onCreate()
        d { "onCreate" }
        notificationManager.createChannel()

        val host = settingsStore.getHost() ?: getString(com.dsp.ping.R.string.app_name)
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notificationManager.build(host, null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        acquireWakeLock()

        // Будильник следующей границы ставится сразу: даже если стартовый пинг
        // не завершится (процесс убит), цепочка 5-минутных пингов сохранится.
        scheduleNextAlarm()

        // Пинг при старте: сразу, не дожидаясь ближайшей границы периода.
        disposer.add(Schedulers.io().scheduleDirect { performPing() })

        // Регулярные пинги: строго по границам периода опроса.
        startPeriodicTimer()

        // Смена периода в Settings применяется без перезапуска сервиса.
        settingsStore.onIntervalChange { startPeriodicTimer() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_PING -> disposer.add(
                Schedulers.io().scheduleDirect { performPing() }
            )

            ACTION_PING_NOW -> disposer.add(
                Schedulers.io().scheduleDirect { performPing(force = true) }
            )

            ACTION_CLOSE -> shutdown()
            else -> {
                // ACTION_START / рестарт системы (null-intent): сервис уже работает.
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        settingsStore.onIntervalChange(null)
        disposer.dispose()
        releaseWakeLock()
        cancelAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun performPing(force: Boolean = false) {
        d { "=>performPing force=$force sd=$isShutdown" }
        if (isShutdown) return

        // Guard и фиксация времени должны быть атомарными: на границе периода
        // Rx-тик и будильник срабатывают одновременно и без синхронизации
        // оба прошли бы проверку → двойной пинг.
        val now = synchronized(this) {
            val current = System.currentTimeMillis()
            val intervalMs = pingIntervalMs()
            // Дубль одной границы распознаём двумя способами:
            // 1) тот же слот границы current / intervalMs — покрывает будильник,
            //    задержанный в Doze на минуты (setAndAllowWhileIdle без exact-разрешения);
            // 2) малое окно по времени — покрывает расхождение Rx/AlarmManager
            //    в несколько мс вокруг границы (Rx считает по nanoTime).
            // Слоты стартового/ручного пинга всегда строго до следующей границы,
            // поэтому свежий ручной пинг не может погасить саму границу периода
            // (баг: пропуск 12:50 после ручного пинга в 12:47:54).
            if (!force && (current / intervalMs == lastPingSlot ||
                    current - lastPingAt < dedupGraceMs(intervalMs))) {
                // Дубликат от параллельных планировщиков одной границы:
                // пинг пропускаем, но цепочку будильников не разрываем.
                if (!isShutdown) scheduleNextAlarm()
                return
            }
            lastPingAt = current
            lastPingSlot = current / intervalMs
            current
        }

        // Будильник следующей границы ставится синхронно: цепочка не должна зависеть
        // от асинхронной записи в БД — процесс может быть убит до колбэка,
        // и 5-минутные пинги оборвались бы.
        if (!isShutdown) scheduleNextAlarm()

        val host = settingsStore.getHost() ?: return

        d { "=performPing 0" }
        val result = if (networkMonitor.isOnline()) {
            pinger.ping(host)
        } else {
            PingResult.NoNetwork
        }
        d { "=performPing 1" }

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
        d { "=performPing $entity" }
        repository.addResult(entity) { onPingPersisted() }
        iconSwitcher.apply(result.toIconStatus())
    }

    /**
     * Пост-действия после фактической записи результата в БД: обновление нотификации
     * и оповещение открытого UI (любой пинг — стартовый, по расписанию или ручной).
     * Перепланирование будильника здесь НЕ выполняется — оно синхронно в [performPing].
     */
    private fun onPingPersisted() {
        d { "onPingPersisted" }
        if (isShutdown) return
        val host = settingsStore.getHost() ?: return

        repository.requestAvailability24h { availability ->
            updateNotification(
                host,
                AvailabilityCalculator.percent(availability.ok, availability.fail)
            )
        }

        sendBroadcast(
            Intent(ACTION_PING_COMPLETED)
                .setPackage(packageName)
        )
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
        d { "shutdown" }
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
        d { "scheduleNextAlarm" }
        if (isShutdown) return
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent()
        val triggerAt = nextBoundaryTime()

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
        d { "cancelAlarm" }
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, PingAlarmReceiver::class.java).setAction(ACTION_PING),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    //endregion

    //region period alignment

    /**
     * Пересоздаёт Rx-таймер регулярных пингов по текущему периоду из настроек.
     * Вызывается при старте сервиса и при каждой смене периода в Settings.
     */
    private fun startPeriodicTimer() {
        periodicTimer?.dispose()
        val intervalMs = pingIntervalMs()
        periodicTimer = Observable.interval(
            delayToNextBoundaryMs(intervalMs),
            intervalMs,
            TimeUnit.MILLISECONDS
        )
            .observeOn(Schedulers.io())
            .subscribe { performPing() }
            .also { disposer.add(it) }
    }

    /**
     * Окно подавления дубля вокруг границы: единицы мс расхождения Rx/Alarm —
     * но не больше половины периода, иначе при периоде 1 сек оно гасило бы
     * сами границы (10 с по умолчанию — это 10 подряд пропущенных пингов).
     */
    private fun dedupGraceMs(intervalMs: Long): Long =
        minOf(DEDUP_GRACE_MS, intervalMs / 2)

    /**
     * Ближайшая следующая граница периода опроса — момент wall-clock, кратный
     * [intervalMs]. Эпоха (1970-01-01 00:00:00 UTC) кратна минуте, а смещения
     * часовых поясов кратны 15 минутам, поэтому выравнивание по эпохе даёт границы,
     * круглые и в локальном времени (для периодов, кратных минуте).
     */
    private fun nextBoundaryTime(
        now: Long = System.currentTimeMillis(),
        intervalMs: Long = pingIntervalMs()
    ): Long = (now / intervalMs + 1) * intervalMs

    /** Задержка от текущего момента до ближайшей следующей границы периода. */
    private fun delayToNextBoundaryMs(
        now: Long = System.currentTimeMillis(),
        intervalMs: Long = pingIntervalMs()
    ): Long {
        val result = nextBoundaryTime(now, intervalMs) - now
        d {"delayToNextBoundaryMs $result"}
        return result
    }

    /** Текущий период опроса из настроек в миллисекундах. */
    private fun pingIntervalMs(): Long =
        settingsStore.getIntervalSec() * 1_000L

    //endregion

    companion object {
        const val ACTION_START = "com.dsp.ping.action.START"
        const val ACTION_PING = "com.dsp.ping.action.PING"
        const val ACTION_PING_NOW = "com.dsp.ping.action.PING_NOW"
        const val ACTION_CLOSE = "com.dsp.ping.action.CLOSE"
        const val ACTION_PING_COMPLETED = "com.dsp.ping.action.PING_COMPLETED"
        const val EXTRA_FROM_NOTIFICATION = "com.dsp.ping.extra.FROM_NOTIFICATION"
        const val NOTIF_ID = 1001

        /**
         * Окно подавления дубля одной границы вокруг самого момента границы:
         * Rx-тик и будильник могут разойтись на единицы миллисекунд из-за разных
         * часов (nanoTime vs RTC). Дубли, задержанные на минуты, ловятся слотом
         * границы (см. [performPing]), поэтому окно не должно быть большим —
         * иначе оно гасит саму границу после свежего ручного пинга.
         */
        private const val DEDUP_GRACE_MS = 10_000L
        private const val KILL_DELAY_MS = 300L
    }
}
