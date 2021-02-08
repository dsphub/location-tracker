package com.dsp.androidsample.di

import com.dsp.androidsample.data.events.EventRepository
import com.dsp.androidsample.ui.history.HistoryViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.Executors

val eventModule = module {
    single { EventRepository.getInstance(androidContext(), Executors.newSingleThreadExecutor()) }
}

val historyModule = module {
    factory {
        HistoryViewModel(
            androidApplication(),
            get()
        )
    }
}
