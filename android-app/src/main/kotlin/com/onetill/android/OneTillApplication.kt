package com.onetill.android

import android.app.Application
import com.onetill.android.di.androidModule
import com.onetill.android.di.connectivityModule
import com.onetill.shared.di.databaseModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OneTillApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        startKoin {
            androidContext(this@OneTillApplication)
            modules(
                androidModule,
                connectivityModule,
                databaseModule,
            )
        }
    }
}
