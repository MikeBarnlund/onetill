package com.onetill.android.di

import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.di.backendModule
import com.onetill.shared.di.cartModule
import com.onetill.shared.di.syncModule
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

private var loadedConfigModules: List<Module>? = null
private var viewModelModuleLoaded = false

fun loadPostWizardModules(config: StoreConfig) {
    val configModules = listOf(
        backendModule(config),
        syncModule,
        cartModule(config.currency),
    )
    loadedConfigModules = configModules

    val toLoad = if (!viewModelModuleLoaded) {
        viewModelModuleLoaded = true
        configModules + postWizardViewModelModule
    } else {
        configModules
    }

    loadKoinModules(toLoad)
}

fun reloadPostWizardModules(config: StoreConfig) {
    loadedConfigModules?.let { unloadKoinModules(it) }
    loadPostWizardModules(config)
}
