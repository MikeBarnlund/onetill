package com.onetill.shared.di

import com.onetill.shared.cart.CartManager
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.local.SqlDelightLocalDataSource
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.db.createDatabase
import com.onetill.shared.ecommerce.ECommerceBackend
import com.onetill.shared.ecommerce.woocommerce.WooCommerceBackend
import com.onetill.shared.ecommerce.woocommerce.WooCommerceClient
import com.onetill.shared.ecommerce.woocommerce.createWooCommerceHttpClient
import com.onetill.shared.setup.SetupManager
import com.onetill.shared.sync.OrderSyncManager
import com.onetill.shared.sync.ProductSyncManager
import com.onetill.shared.sync.SyncOrchestrator
import org.koin.dsl.module

val databaseModule = module {
    single { createDatabase(get()) }
    single<LocalDataSource> { SqlDelightLocalDataSource(get()) }
    single {
        SetupManager(
            localDataSource = get(),
            backendFactory = { config -> WooCommerceBackend(config) },
        )
    }
}

fun backendModule(config: StoreConfig) = module {
    single { config }
    single { createWooCommerceHttpClient(config) }
    single { WooCommerceClient(get()) }
    single<ECommerceBackend> { WooCommerceBackend(get(), get()) }
}

val syncModule = module {
    single { ProductSyncManager(get(), get()) }
    single { OrderSyncManager(get(), get()) }
    single { SyncOrchestrator(get(), get(), get(), get()) }
}

fun cartModule(currency: String) = module {
    factory { CartManager(get(), currency, get()) }
}
