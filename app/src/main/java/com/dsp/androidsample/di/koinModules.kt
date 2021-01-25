package com.dsp.androidsample.di

import com.dsp.androidsample.ui.HistoryViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val historyModule = module {
    factory { HistoryViewModel(androidApplication()) }
}
