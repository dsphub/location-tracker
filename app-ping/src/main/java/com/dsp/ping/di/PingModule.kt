package com.dsp.ping.di

import com.dsp.ping.data.PingRepository
import com.dsp.ping.data.SettingsStore
import com.dsp.ping.data.db.PingDatabase
import com.dsp.ping.util.AppExecutors
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val pingModule = module {
    single { PingDatabase.getInstance(androidContext()) }
    single { get<PingDatabase>().pingDao() }
    single { PingRepository(get(), AppExecutors.diskIo()) }
    single { SettingsStore(androidContext()) }
}
