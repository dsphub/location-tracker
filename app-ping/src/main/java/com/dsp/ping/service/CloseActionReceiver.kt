package com.dsp.ping.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Обрабатывает нажатие «Close» в нотификации: направляет ACTION_CLOSE в [PingService].
 */
class CloseActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.startService(
            Intent(context, PingService::class.java).setAction(PingService.ACTION_CLOSE)
        )
    }
}
