package com.dsp.ping.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dsp.ping.data.SettingsStore

/**
 * Автостарт мониторинга после загрузки устройства (опция «Автостарт» в Settings).
 * Только при включённой опции и заданном хосте.
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
