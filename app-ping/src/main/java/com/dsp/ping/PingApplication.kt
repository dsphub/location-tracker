package com.dsp.ping

import android.app.Application
import com.dsp.ping.di.pingModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PingApplication)
            modules(
                listOf(
                    pingModule
                )
            )
        }
    }
}
