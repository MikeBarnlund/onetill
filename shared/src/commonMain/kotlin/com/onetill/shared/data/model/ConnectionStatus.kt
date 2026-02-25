package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ConnectionStatus {
    @Serializable
    data class Connected(val storeName: String) : ConnectionStatus()

    @Serializable
    data object InvalidCredentials : ConnectionStatus()

    @Serializable
    data class NetworkError(val message: String) : ConnectionStatus()

    @Serializable
    data object StoreNotFound : ConnectionStatus()
}
