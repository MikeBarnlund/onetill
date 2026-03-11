package com.onetill.shared.cart

import com.onetill.shared.data.model.Money

data class CartState(
    val items: List<CartItem>,
    val appliedCoupons: List<AppliedCoupon>,
    val customerId: Long?,
    val note: String?,
    val currency: String,
    val subtotal: Money,
    val discountTotal: Money,
    val estimatedTax: Money,
    val estimatedTotal: Money,
    val itemCount: Int,
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val couponCodes: List<String> get() = appliedCoupons.map { it.code }

    companion object {
        fun empty(currency: String) = CartState(
            items = emptyList(),
            appliedCoupons = emptyList(),
            customerId = null,
            note = null,
            currency = currency,
            subtotal = Money.zero(currency),
            discountTotal = Money.zero(currency),
            estimatedTax = Money.zero(currency),
            estimatedTotal = Money.zero(currency),
            itemCount = 0,
        )
    }
}
