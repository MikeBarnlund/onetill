package com.onetill.shared.setup

import com.onetill.shared.data.model.StoreConfig

sealed class SetupState {
    data object Idle : SetupState()
    data object Validating : SetupState()
    data class Validated(val storeName: String, val currency: String) : SetupState()
    data object Saving : SetupState()
    data class Complete(val config: StoreConfig) : SetupState()
    data class Error(val message: String) : SetupState()
}
