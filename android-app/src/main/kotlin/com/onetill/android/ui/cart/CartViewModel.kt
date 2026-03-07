package com.onetill.android.ui.cart

import androidx.lifecycle.ViewModel
import com.onetill.shared.cart.CartManager
import com.onetill.shared.cart.CartState
import kotlinx.coroutines.flow.StateFlow

class CartViewModel(
    private val cartManager: CartManager,
) : ViewModel() {

    val cartState: StateFlow<CartState> = cartManager.cartState

    fun updateQuantity(productId: Long, variantId: Long?, newQuantity: Int) {
        cartManager.updateQuantity(productId, variantId, newQuantity)
    }

    fun removeItem(productId: Long, variantId: Long?) {
        cartManager.removeItem(productId, variantId)
    }

    fun clearCart() {
        cartManager.clearCart()
    }

    fun applyCoupon(code: String) {
        cartManager.applyCoupon(code)
    }

    fun removeCoupon(code: String) {
        cartManager.removeCoupon(code)
    }
}
