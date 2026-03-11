package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Coupon(
    val id: Long,
    val code: String,
    val type: CouponType,
    val amount: String,
)

@Serializable
enum class CouponType {
    PERCENT,
    FIXED_CART,
    FIXED_PRODUCT,
}
