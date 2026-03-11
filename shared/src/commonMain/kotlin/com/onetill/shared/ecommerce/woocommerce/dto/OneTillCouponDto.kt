package com.onetill.shared.ecommerce.woocommerce.dto

import com.onetill.shared.data.model.Coupon
import com.onetill.shared.data.model.CouponType
import kotlinx.serialization.Serializable

@Serializable
data class OneTillCouponDto(
    val id: Long,
    val code: String,
    val type: String,
    val amount: String,
)

fun OneTillCouponDto.toDomain(): Coupon = Coupon(
    id = id,
    code = code.uppercase(),
    type = when (type) {
        "percent" -> CouponType.PERCENT
        "fixed_cart" -> CouponType.FIXED_CART
        "fixed_product" -> CouponType.FIXED_PRODUCT
        else -> CouponType.FIXED_CART
    },
    amount = amount,
)
