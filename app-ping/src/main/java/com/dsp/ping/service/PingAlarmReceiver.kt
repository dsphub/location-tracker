package com.dsp.ping.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Будильник Doze: перезапускает сервис для выполнения пинга.
 */
class PingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, PingService::class.java).setAction(PingService.ACTION_PING)
        )
    }
}
