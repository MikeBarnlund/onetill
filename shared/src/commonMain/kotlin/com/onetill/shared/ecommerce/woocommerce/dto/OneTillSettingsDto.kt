package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTillSettingsDto(
    val tax: OneTillTaxSettingsDto = OneTillTaxSettingsDto(),
)

@Serializable
data class OneTillTaxSettingsDto(
    val enabled: Boolean = false,
    @SerialName("prices_include_tax") val pricesIncludeTax: Boolean = false,
    @SerialName("tax_rates") val taxRates: List<OneTillTaxRateDto> = emptyList(),
)

@Serializable
data class OneTillTaxRateDto(
    val id: Long,
    val country: String = "",
    val state: String = "",
    val rate: String = "0",
    val name: String = "",
    val shipping: Boolean = false,
    val compound: Boolean = false,
    @SerialName("class") val taxClass: String = "standard",
)
