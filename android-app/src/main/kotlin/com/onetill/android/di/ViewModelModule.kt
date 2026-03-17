package com.onetill.android.di

import com.onetill.android.ui.cart.CartViewModel
import com.onetill.android.ui.catalog.CatalogViewModel
import com.onetill.android.ui.checkout.CheckoutViewModel
import com.onetill.android.ui.lock.LockViewModel
import com.onetill.android.ui.orders.OrdersViewModel
import com.onetill.android.ui.receipt.ReceiptEmailViewModel
import com.onetill.android.ui.scanner.QrPairingViewModel
import com.onetill.android.ui.settings.OfflinePaymentSettingsViewModel
import com.onetill.android.ui.settings.SettingsViewModel
import com.onetill.android.ui.setup.SetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val setupViewModelModule = module {
    viewModel { SetupViewModel(get(), get(), get()) }
    viewModel { LockViewModel(get()) }
}

val postWizardViewModelModule = module {
    viewModel { CatalogViewModel(get(), get(), get()) }
    viewModel { CartViewModel(get()) }
    viewModel { CheckoutViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ReceiptEmailViewModel(get(), get()) }
    viewModel { OrdersViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { QrPairingViewModel(get(), get(), get()) }
    viewModel { OfflinePaymentSettingsViewModel(get(), get()) }
}
