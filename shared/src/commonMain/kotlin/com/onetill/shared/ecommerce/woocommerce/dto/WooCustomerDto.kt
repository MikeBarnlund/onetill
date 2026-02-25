package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WooCustomerDto(
    val id: Long,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val email: String = "",
    val billing: WooBillingDto? = null,
)

@Serializable
data class WooBillingDto(
    val phone: String = "",
)

/**
 * Request body for creating a customer via POST /customers.
 */
@Serializable
data class WooCreateCustomerDto(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String = "",
    val billing: WooCreateBillingDto? = null,
)

@Serializable
data class WooCreateBillingDto(
    val phone: String = "",
)
