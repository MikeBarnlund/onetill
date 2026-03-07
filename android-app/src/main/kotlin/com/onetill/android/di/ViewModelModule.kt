package com.onetill.android.di

import com.onetill.android.ui.cart.CartViewModel
import com.onetill.android.ui.catalog.CatalogViewModel
import com.onetill.android.ui.checkout.CheckoutViewModel
import com.onetill.android.ui.orders.OrdersViewModel
import com.onetill.android.ui.settings.SettingsViewModel
import com.onetill.android.ui.setup.SetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val setupViewModelModule = module {
    viewModel { SetupViewModel(get(), get()) }
}

val postWizardViewModelModule = module {
    viewModel { CatalogViewModel(get(), get(), get()) }
    viewModel { CartViewModel(get()) }
    viewModel { CheckoutViewModel(get(), get(), get(), get()) }
    viewModel { OrdersViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
