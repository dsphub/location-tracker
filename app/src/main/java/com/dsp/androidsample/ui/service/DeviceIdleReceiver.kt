package com.dsp.androidsample.ui.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

@RequiresApi(Build.VERSION_CODES.M)
class DeviceIdleReceiver : BroadcastReceiver() {
    private val _subject: BehaviorSubject<Pair<Boolean, Boolean>> = BehaviorSubject.create()
    val observable: Observable<Pair<Boolean, Boolean>> = _subject

    override fun onReceive(context: Context?, intent: Intent?) {
        val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        _subject.onNext(pm.isPowerSaveMode to pm.isDeviceIdleMode)
    }
}