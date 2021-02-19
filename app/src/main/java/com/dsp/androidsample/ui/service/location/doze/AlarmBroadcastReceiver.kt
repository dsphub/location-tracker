package com.dsp.androidsample.ui.service.location.doze

import android.content.Context
import android.content.Intent
import androidx.legacy.content.WakefulBroadcastReceiver
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.ui.service.LocationService
import com.dsp.androidsample.ui.service.LocationService.Companion.EXTRA_ACTION_WAKEUP

class AlarmBroadcastReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        d { "onReceive" }
        val intent = Intent(context, LocationService::class.java).apply {
            putExtra(EXTRA_ACTION_WAKEUP, true)
        }
        startWakefulService(context, intent)
    }
}