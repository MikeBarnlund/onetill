package com.onetill.android.di

import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.di.backendModule
import com.onetill.shared.di.cartModule
import com.onetill.shared.di.syncModule
import org.koin.core.context.loadKoinModules

fun loadPostWizardModules(config: StoreConfig) {
    loadKoinModules(
        listOf(
            backendModule(config),
            syncModule,
            cartModule(config.currency),
        )
    )
}
