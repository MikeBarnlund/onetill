package com.onetill.android.di

import com.onetill.android.AndroidConnectivityMonitor
import com.onetill.shared.db.DatabaseDriverFactory
import com.onetill.shared.sync.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { DatabaseDriverFactory(androidContext()) }
}

val connectivityModule = module {
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(androidContext()) }
}
