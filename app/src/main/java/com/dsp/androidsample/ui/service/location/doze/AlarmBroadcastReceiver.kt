package com.dsp.androidsample.ui.service.location.doze

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.legacy.content.WakefulBroadcastReceiver
import com.dsp.androidsample.BuildConfig
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.ui.service.LocationService
import com.dsp.androidsample.ui.service.LocationService.Companion.EXTRA_ACTION_WAKEUP

class AlarmBroadcastReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        d { "onReceive" }
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "${BuildConfig.APPLICATION_ID}:dozeLockTag"
        )
        wl.acquire(10 * 60 * 1000L /*10 minutes*/)
        val intent = Intent(context, LocationService::class.java).apply {
            putExtra(EXTRA_ACTION_WAKEUP, true)
        }
        startWakefulService(context, intent)
        wl.release()
    }
}