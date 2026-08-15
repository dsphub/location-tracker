package com.dsp.ping.di

import com.dsp.ping.data.PingRepository
import com.dsp.ping.data.SettingsStore
import com.dsp.ping.data.network.NetworkMonitor
import com.dsp.ping.data.db.PingDatabase
import com.dsp.ping.icons.IconSwitcher
import com.dsp.ping.notifications.PingNotificationManager
import com.dsp.ping.ping.HttpPinger
import com.dsp.ping.util.AppExecutors
import com.dsp.ping.ui.PingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val pingModule = module {
    single { PingDatabase.getInstance(androidContext()) }
    single { get<PingDatabase>().pingDao() }
    single { PingRepository(get(), AppExecutors.diskIo()) }
    single { SettingsStore(androidContext()) }
    single { NetworkMonitor(androidContext()) }
    single { HttpPinger() }
    single { PingNotificationManager(androidContext()) }
    single { IconSwitcher(androidContext()) }
    factory { PingViewModel(get(), get()) }
}
