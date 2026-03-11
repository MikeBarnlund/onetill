package com.onetill.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory

import com.onetill.android.di.androidModule
import com.onetill.android.di.connectivityModule
import com.onetill.android.di.setupViewModelModule
import com.onetill.shared.di.databaseModule
import com.stripe.stripeterminal.TerminalApplicationDelegate
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OneTillApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()

        TerminalApplicationDelegate.onCreate(this)
        Napier.base(DebugAntilog())

        startKoin {
            androidContext(this@OneTillApplication)
            modules(
                androidModule,
                connectivityModule,
                databaseModule,
                setupViewModelModule,
            )
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { HttpClient(OkHttp) }))
            }
            .build()
    }
}
