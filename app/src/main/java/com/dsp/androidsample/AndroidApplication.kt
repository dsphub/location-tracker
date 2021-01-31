package com.dsp.androidsample

import android.app.Application
import com.dsp.androidsample.di.eventModule
import com.dsp.androidsample.di.historyModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AndroidApplication)
            modules(
                listOf(
                    eventModule,
                    historyModule
                )
            )
        }
    }
}