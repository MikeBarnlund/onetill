package com.onetill.shared.cart

import com.onetill.shared.data.model.CouponType
import com.onetill.shared.data.model.Money

data class AppliedCoupon(
    val code: String,
    val type: CouponType,
    val amount: String,
    val discountAmount: Money,
)

sealed class CouponApplyResult {
    data class Applied(val code: String, val discountAmount: Money) : CouponApplyResult()
    data class Invalid(val reason: String) : CouponApplyResult()
}
