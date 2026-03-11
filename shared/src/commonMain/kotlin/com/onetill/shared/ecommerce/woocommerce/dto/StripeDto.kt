package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.Serializable

@Serializable
data class StripeConnectionTokenResponse(val secret: String)
