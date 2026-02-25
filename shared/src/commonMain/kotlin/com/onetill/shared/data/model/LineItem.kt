package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LineItem(
    val id: Long?,
    val productId: Long,
    val variantId: Long?,
    val name: String,
    val sku: String?,
    val quantity: Int,
    val unitPrice: Money,
    val totalPrice: Money,
)
