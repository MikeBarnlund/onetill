package com.onetill.android.ui.cart

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CartItemUi(
    val id: String,
    val name: String,
    val variationInfo: String?,
    val imageUrl: String?,
    val quantity: Int,
    val unitPriceCents: Long,
) {
    val totalPriceCents: Long get() = unitPriceCents * quantity
}

data class CartUiState(
    val items: List<CartItemUi>,
    val couponCode: String?,
    val discountCents: Long,
    val taxRate: Double,
) {
    val subtotalCents: Long get() = items.sumOf { it.totalPriceCents }
    val taxCents: Long get() = ((subtotalCents - discountCents) * taxRate).toLong()
    val totalCents: Long get() = subtotalCents - discountCents + taxCents
    val itemCount: Int get() = items.sumOf { it.quantity }
}

class CartViewModel : ViewModel() {

    private val fakeItems = listOf(
        CartItemUi("1", "Organic Honey 500g", null, null, 2, 1499),
        CartItemUi("2", "Sourdough Bread Loaf", null, null, 1, 899),
        CartItemUi("3", "Artisan Cheese Wheel", "Aged Cheddar", null, 1, 2499),
    )

    private val _cartState = MutableStateFlow(
        CartUiState(
            items = fakeItems,
            couponCode = null,
            discountCents = 0,
            taxRate = 0.10,
        ),
    )
    val cartState: StateFlow<CartUiState> = _cartState.asStateFlow()

    fun updateQuantity(itemId: String, newQuantity: Int) {
        _cartState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.id == itemId) it.copy(quantity = newQuantity) else it
                },
            )
        }
    }

    fun removeItem(itemId: String) {
        _cartState.update { state ->
            state.copy(items = state.items.filter { it.id != itemId })
        }
    }

    fun clearCart() {
        _cartState.update { it.copy(items = emptyList(), couponCode = null, discountCents = 0) }
    }

    fun applyCoupon(code: String) {
        if (code.equals("SAVE10", ignoreCase = true)) {
            _cartState.update { state ->
                state.copy(
                    couponCode = code,
                    discountCents = (state.subtotalCents * 0.10).toLong(),
                )
            }
        }
    }

    fun removeCoupon() {
        _cartState.update { it.copy(couponCode = null, discountCents = 0) }
    }

    companion object {
        fun formatCents(cents: Long): String {
            val dollars = cents / 100
            val remainder = cents % 100
            return "$${dollars}.${remainder.toString().padStart(2, '0')}"
        }
    }
}
