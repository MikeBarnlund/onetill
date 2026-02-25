package com.onetill.shared.cart

import com.onetill.shared.data.model.Money

data class CartState(
    val items: List<CartItem>,
    val couponCodes: List<String>,
    val customerId: Long?,
    val note: String?,
    val currency: String,
    val subtotal: Money,
    val estimatedTax: Money,
    val estimatedTotal: Money,
    val itemCount: Int,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    companion object {
        fun empty(currency: String) = CartState(
            items = emptyList(),
            couponCodes = emptyList(),
            customerId = null,
            note = null,
            currency = currency,
            subtotal = Money.zero(currency),
            estimatedTax = Money.zero(currency),
            estimatedTotal = Money.zero(currency),
            itemCount = 0,
        )
    }
}
