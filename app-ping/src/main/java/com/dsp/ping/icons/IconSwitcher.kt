package com.dsp.ping.icons

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.concurrent.atomic.AtomicInteger

/**
 * Переключает иконку приложения на лаунчере по статусу пинга через
 * включение/выключение activity-alias из манифеста (AliasGreen/AliasRed/AliasGray).
 *
 * Отключение алиаса, которому принадлежит видимая активити, завершает её системой
 * (DONT_KILL_APP спасает только процесс) — приложение "закрывается само". Поэтому
 * пока активити на экране, переключение откладывается: [apply] запоминает статус
 * в pending, и он применяется в момент, когда последняя активити уходит со экрана.
 *
 * Работает только на API 26+ (adaptive icons в mipmap-anydpi-v26). На API 21–25
 * [apply] — no-op: aliases указывают на ресурсы, которых в младших версиях нет,
 * поэтому приложение всегда показывает базовую серую иконку, заданную в
 * `<application android:icon="@drawable/ic_globe_gray">` в манифесте.
 *
 * Включённый alias персистентен ([PackageManager.setComponentEnabledSetting]
 * переживает перезапуск процесса), кэш [current] — нет: после рестарта процесса
 * первый [apply] повторно применит тот же статус (безопасно, но с лишним вызовом
 * PackageManager — сознательная плата за простоту).
 */
class IconSwitcher(private val context: Context) {

    @Volatile
    private var current: IconStatus? = null

    @Volatile
    private var pending: IconStatus? = null

    private val startedActivities = AtomicInteger()

    init {
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivities.incrementAndGet()
                }

                override fun onActivityStopped(activity: Activity) {
                    // Последняя активити ушла со экрана — можно применить отложенное.
                    if (startedActivities.decrementAndGet() == 0) {
                        pending?.let(::doApply)
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    fun apply(status: IconStatus) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (current == status) return

        if (startedActivities.get() > 0) {
            pending = status
            return
        }
        doApply(status)
    }

    private fun doApply(status: IconStatus) {
        val pm = context.packageManager
        val targetClass = ALIAS_CLASSES.getValue(status)

        pm.setComponentEnabledSetting(
            ComponentName(context, targetClass),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        ALIAS_CLASSES.values
            .filterNot { it == targetClass }
            .forEach { aliasClass ->
                pm.setComponentEnabledSetting(
                    ComponentName(context, aliasClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        current = status
        pending = null
    }

    companion object {
        // Полные имена классов alias: namespace com.dsp.ping, package приложения
        // ComponentName возьмёт из context (applicationId com.dsp.pingtracker).
        private val ALIAS_CLASSES = mapOf(
            IconStatus.GREEN to "com.dsp.ping.ui.AliasGreen",
            IconStatus.RED to "com.dsp.ping.ui.AliasRed",
            IconStatus.GRAY to "com.dsp.ping.ui.AliasGray"
        )
    }
}
