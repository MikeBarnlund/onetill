package com.onetill.shared.cart

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductVariant
import com.onetill.shared.data.model.TaxRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CartManager(
    private val localDataSource: LocalDataSource,
    private val currency: String,
    private val scope: CoroutineScope,
) {
    private val taxCalculator = TaxCalculator()

    private var items = mutableListOf<CartItem>()
    private var couponCodes = mutableListOf<String>()
    private var customerId: Long? = null
    private var note: String? = null
    private var cachedTaxRates: List<TaxRate> = emptyList()

    private val _cartState = MutableStateFlow(CartState.empty(currency))
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    init {
        scope.launch { refreshTaxRates() }
    }

    fun addProduct(product: Product, variant: ProductVariant? = null) {
        val newItem = if (variant != null) {
            variant.toCartItem(product)
        } else {
            product.toCartItem()
        }

        val existingIndex = items.indexOfFirst {
            it.productId == newItem.productId && it.variantId == newItem.variantId
        }

        if (existingIndex >= 0) {
            val existing = items[existingIndex]
            items[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            items.add(newItem)
        }

        emitState()
    }

    fun removeItem(productId: Long, variantId: Long? = null) {
        items.removeAll { it.productId == productId && it.variantId == variantId }
        emitState()
    }

    fun updateQuantity(productId: Long, variantId: Long?, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItem(productId, variantId)
            return
        }

        val index = items.indexOfFirst {
            it.productId == productId && it.variantId == variantId
        }
        if (index >= 0) {
            items[index] = items[index].copy(quantity = newQuantity)
            emitState()
        }
    }

    fun applyCoupon(code: String) {
        val normalized = code.trim().uppercase()
        if (normalized.isNotEmpty() && couponCodes.none { it.equals(normalized, ignoreCase = true) }) {
            couponCodes.add(normalized)
            emitState()
        }
    }

    fun removeCoupon(code: String) {
        couponCodes.removeAll { it.equals(code.trim(), ignoreCase = true) }
        emitState()
    }

    fun setCustomer(customerId: Long?) {
        this.customerId = customerId
        emitState()
    }

    fun setNote(note: String?) {
        this.note = note?.ifBlank { null }
        emitState()
    }

    fun clearCart() {
        items.clear()
        couponCodes.clear()
        customerId = null
        note = null
        emitState()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun buildOrderDraft(paymentMethod: PaymentMethod): OrderDraft = OrderDraft(
        lineItems = items.map { it.toLineItem() },
        customerId = customerId,
        paymentMethod = paymentMethod,
        idempotencyKey = Uuid.random().toString(),
        note = note,
        couponCodes = couponCodes.toList(),
    )

    suspend fun refreshTaxRates() {
        cachedTaxRates = localDataSource.getAllTaxRates()
        emitState()
    }

    private fun emitState() {
        val subtotal = if (items.isEmpty()) {
            Money.zero(currency)
        } else {
            items.fold(Money.zero(currency)) { acc, item -> acc + item.totalPrice }
        }

        val estimatedTax = taxCalculator.calculateTax(subtotal, cachedTaxRates)
        val estimatedTotal = subtotal + estimatedTax

        _cartState.value = CartState(
            items = items.toList(),
            couponCodes = couponCodes.toList(),
            customerId = customerId,
            note = note,
            currency = currency,
            subtotal = subtotal,
            estimatedTax = estimatedTax,
            estimatedTotal = estimatedTotal,
            itemCount = items.sumOf { it.quantity },
        )
    }
}
