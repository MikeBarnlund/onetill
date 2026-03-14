package com.onetill.android.ui.cart

import androidx.lifecycle.ViewModel
import com.onetill.shared.cart.CartManager
import com.onetill.shared.cart.CartState
import com.onetill.shared.cart.CouponApplyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartViewModel(
    private val cartManager: CartManager,
) : ViewModel() {

    val cartState: StateFlow<CartState> = cartManager.cartState

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    fun updateQuantity(productId: Long, variantId: Long?, newQuantity: Int) {
        cartManager.updateQuantity(productId, variantId, newQuantity)
    }

    fun removeItem(productId: Long, variantId: Long?) {
        cartManager.removeItem(productId, variantId)
    }

    fun clearCart() {
        cartManager.clearCart()
    }

    fun applyCoupon(code: String): Boolean {
        return when (val result = cartManager.applyCoupon(code)) {
            is CouponApplyResult.Applied -> {
                _couponError.value = null
                true
            }
            is CouponApplyResult.Invalid -> {
                _couponError.value = result.reason
                false
            }
        }
    }

    fun clearCouponError() {
        _couponError.value = null
    }

    fun removeCoupon(code: String) {
        cartManager.removeCoupon(code)
    }

    fun removeCustomSale(id: String) {
        cartManager.removeCustomSale(id)
    }
}
