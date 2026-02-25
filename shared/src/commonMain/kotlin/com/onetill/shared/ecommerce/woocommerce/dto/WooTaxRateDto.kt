package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.Serializable

@Serializable
data class WooTaxRateDto(
    val id: Long,
    val name: String = "",
    val rate: String = "0",
    val country: String = "",
    val state: String = "",
    val compound: Boolean = false,
    val shipping: Boolean = false,
)
