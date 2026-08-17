package com.dsp.ping.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dsp.ping.data.SettingsStore

/**
 * Автостарт мониторинга после загрузки устройства (опция «Автостарт» в Settings).
 *
 * Запускается только при включённой опции и заданном хосте: без хоста сервис
 * работал бы вхолостую — пинги не выполняются, пользователь попадает на Setup.
 *
 * BOOT_COMPLETED даёт временное исключение из запрета старта foreground-сервисов
 * из бэкграунда (Android 12+), поэтому старт из ресивера легитимен.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val settings = SettingsStore(context)
        if (!settings.isAutostartEnabled()) return
        if (settings.getHost() == null) return

        ContextCompat.startForegroundService(
            context,
            Intent(context, PingService::class.java).setAction(PingService.ACTION_START)
        )
    }
}
