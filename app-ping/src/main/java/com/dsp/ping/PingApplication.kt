package com.dsp.ping

import android.app.Application
import com.dsp.ping.di.pingModule
import com.dsp.ping.icons.IconSwitcher
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class PingApplication : Application(), KoinComponent {

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
        // Создаём до старта первой активити: IconSwitcher считает видимые активити
        // через lifecycle-callbacks и откладывает переключение alias-иконки, пока
        // приложение на экране (иначе система завершит активити отключённого alias).
        get<IconSwitcher>()
    }
}
