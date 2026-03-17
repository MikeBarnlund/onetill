package com.onetill.shared.cart

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Coupon
import com.onetill.shared.data.model.CouponType
import com.onetill.shared.data.model.FeeLine
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductVariant
import com.onetill.shared.data.model.TaxRate
import com.onetill.shared.ecommerce.ECommerceBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class AddResult { Added, StockLimitReached }

class CartManager(
    private val localDataSource: LocalDataSource,
    private val currency: String,
    private val scope: CoroutineScope,
    private val backend: ECommerceBackend? = null,
) {
    private val taxCalculator = TaxCalculator()

    private var items = mutableListOf<CartItem>()
    private var customSaleItems = mutableListOf<CustomSaleItem>()
    private var appliedCoupons = mutableListOf<AppliedCoupon>()
    private var customerId: Long? = null
    private var note: String? = null
    private var cachedTaxRates: List<TaxRate> = emptyList()
    private var cachedCoupons: List<Coupon> = emptyList()

    private val _cartState = MutableStateFlow(CartState.empty(currency))
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    init {
        scope.launch {
            refreshTaxRates()
            refreshCoupons()
        }
    }

    fun addProduct(product: Product, variant: ProductVariant? = null): AddResult {
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
            val maxQty = existing.maxQuantity
            if (maxQty != null && existing.quantity >= maxQty) {
                return AddResult.StockLimitReached
            }
            items[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            val maxQty = newItem.maxQuantity
            if (maxQty != null && maxQty <= 0) {
                return AddResult.StockLimitReached
            }
            items.add(newItem)
        }

        emitState()
        return AddResult.Added
    }

    fun removeItem(productId: Long, variantId: Long? = null) {
        items.removeAll { it.productId == productId && it.variantId == variantId }
        emitState()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addCustomSale(description: String, amount: Money) {
        customSaleItems.add(
            CustomSaleItem(
                id = Uuid.random().toString(),
                description = description.ifBlank { "Custom Sale" },
                amount = amount,
            ),
        )
        emitState()
    }

    fun removeCustomSale(id: String) {
        customSaleItems.removeAll { it.id == id }
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
            val item = items[index]
            val clamped = item.maxQuantity?.let { newQuantity.coerceAtMost(it) } ?: newQuantity
            items[index] = item.copy(quantity = clamped)
            emitState()
        }
    }

    fun applyCoupon(code: String): CouponApplyResult {
        val normalized = code.trim().uppercase()
        if (normalized.isEmpty()) return CouponApplyResult.Invalid("Enter a coupon code")

        if (appliedCoupons.any { it.code.equals(normalized, ignoreCase = true) }) {
            return CouponApplyResult.Invalid("Coupon already applied")
        }

        val coupon = cachedCoupons.find { it.code.equals(normalized, ignoreCase = true) }
            ?: return CouponApplyResult.Invalid("Invalid coupon code")

        val subtotal = items.fold(Money.zero(currency)) { acc, item -> acc + item.totalPrice }
        val discountAmount = calculateDiscount(coupon, subtotal)

        appliedCoupons.add(
            AppliedCoupon(
                code = coupon.code,
                type = coupon.type,
                amount = coupon.amount,
                discountAmount = discountAmount,
            )
        )

        emitState()
        return CouponApplyResult.Applied(coupon.code, discountAmount)
    }

    fun removeCoupon(code: String) {
        appliedCoupons.removeAll { it.code.equals(code.trim(), ignoreCase = true) }
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

    fun clearCart(sold: Boolean = false) {
        if (sold) {
            // Decrement local stock immediately so the catalog reflects the sale
            val soldItems = items.toList()
            scope.launch {
                for (item in soldItems) {
                    if (item.maxQuantity != null) {
                        localDataSource.decrementStock(item.productId, item.variantId, item.quantity)
                    }
                }
            }
        }
        items.clear()
        customSaleItems.clear()
        appliedCoupons.clear()
        customerId = null
        note = null
        emitState()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun buildOrderDraft(
        paymentMethod: PaymentMethod,
        stripeTransactionId: String? = null,
        cardBrand: String? = null,
        cardLast4: String? = null,
        idempotencyKey: String? = null,
        paymentCreatedOffline: Boolean = false,
        staffName: String? = null,
    ): OrderDraft {
        val state = _cartState.value
        return OrderDraft(
            lineItems = items.map { it.toLineItem() },
            feeLines = customSaleItems.map { FeeLine(name = it.description, amount = it.amount) },
            customerId = customerId,
            paymentMethod = paymentMethod,
            idempotencyKey = idempotencyKey ?: Uuid.random().toString(),
            note = note,
            couponCodes = appliedCoupons.map { it.code },
            discountCents = state.discountTotal.amountCents,
            stripeTransactionId = stripeTransactionId,
            cardBrand = cardBrand,
            cardLast4 = cardLast4,
            paymentCreatedOffline = paymentCreatedOffline,
            estimatedTaxCents = state.estimatedTax.amountCents,
            staffName = staffName,
        )
    }

    /**
     * Fetches server-side tax estimate at checkout time.
     * On success, updates cached rates and cart state with exact tax.
     * On failure (offline), keeps local estimate — returns error for caller to handle.
     */
    suspend fun fetchServerTaxEstimate(): AppResult<Money> {
        val be = backend ?: return AppResult.Error("No backend configured")
        if (items.isEmpty() && customSaleItems.isEmpty()) {
            return AppResult.Success(Money.zero(currency))
        }

        return when (val result = be.estimateTax(items.toList())) {
            is AppResult.Success -> {
                // Update cached rates from server response
                val allRates = result.data.ratesByClass.flatMap { (_, rates) -> rates }
                if (allRates.isNotEmpty()) {
                    cachedTaxRates = allRates
                    localDataSource.saveTaxRates(allRates)
                }
                // Update cart state with exact tax from server
                emitState()
                AppResult.Success(result.data.taxTotal)
            }
            is AppResult.Error -> result
        }
    }

    suspend fun refreshTaxRates() {
        cachedTaxRates = localDataSource.getAllTaxRates()
        emitState()
    }

    suspend fun refreshCoupons() {
        cachedCoupons = localDataSource.getAllCoupons()
    }

    private fun calculateDiscount(coupon: Coupon, subtotal: Money): Money {
        val amountValue = coupon.amount.toDoubleOrNull() ?: 0.0
        val discountCents = when (coupon.type) {
            CouponType.PERCENT -> (subtotal.amountCents * amountValue / 100.0).roundToLong()
            CouponType.FIXED_CART -> (amountValue * 100).roundToLong()
            CouponType.FIXED_PRODUCT -> {
                val itemCount = items.sumOf { it.quantity }
                (amountValue * 100 * itemCount).roundToLong()
            }
        }
        // Never discount more than the subtotal
        return Money(
            amountCents = discountCents.coerceAtMost(subtotal.amountCents),
            currencyCode = subtotal.currencyCode,
        )
    }

    private var serverTaxFetchInFlight = false

    private fun emitState() {
        val hasItems = items.isNotEmpty() || customSaleItems.isNotEmpty()

        // When cached rates are empty and cart has items, fetch from server.
        // This is the only way to discover rates for automated tax services
        // (WooCommerce Tax, TaxJar, Avalara) which don't pre-populate the rates table.
        if (hasItems && cachedTaxRates.isEmpty() && !serverTaxFetchInFlight) {
            serverTaxFetchInFlight = true
            scope.launch {
                fetchServerTaxEstimate()
                serverTaxFetchInFlight = false
            }
        }

        val productSubtotal = items.fold(Money.zero(currency)) { acc, item -> acc + item.totalPrice }
        val customSaleTotal = customSaleItems.fold(Money.zero(currency)) { acc, item -> acc + item.amount }
        val subtotal = productSubtotal + customSaleTotal

        // Recalculate discount amounts when cart changes (e.g. percent coupons scale with subtotal)
        val recalculated = appliedCoupons.map { applied ->
            val coupon = cachedCoupons.find { it.code == applied.code }
            if (coupon != null) {
                applied.copy(discountAmount = calculateDiscount(coupon, subtotal))
            } else {
                applied
            }
        }
        appliedCoupons.clear()
        appliedCoupons.addAll(recalculated)

        val discountTotal = if (appliedCoupons.isEmpty()) {
            Money.zero(currency)
        } else {
            appliedCoupons.fold(Money.zero(currency)) { acc, c -> acc + c.discountAmount }
        }

        // Tax is calculated on the discounted subtotal (matches WooCommerce behavior).
        // Distribute discount proportionally across items, then group by tax class.
        val discountedSubtotal = subtotal - discountTotal
        val discountRatio = if (subtotal.amountCents > 0) {
            discountedSubtotal.amountCents.toDouble() / subtotal.amountCents.toDouble()
        } else {
            1.0
        }

        val taxableItems = items.map { item ->
            TaxableItem(
                subtotalCents = (item.totalPrice.amountCents * discountRatio).roundToLong(),
                taxClass = item.taxClass,
            )
        } + customSaleItems.map { item ->
            TaxableItem(
                subtotalCents = (item.amount.amountCents * discountRatio).roundToLong(),
                taxClass = "", // Custom sales use standard tax class
            )
        }

        val estimatedTax = taxCalculator.calculateTax(taxableItems, cachedTaxRates, currency)
        val estimatedTotal = discountedSubtotal + estimatedTax

        _cartState.value = CartState(
            items = items.toList(),
            customSaleItems = customSaleItems.toList(),
            appliedCoupons = appliedCoupons.toList(),
            customerId = customerId,
            note = note,
            currency = currency,
            subtotal = subtotal,
            discountTotal = discountTotal,
            estimatedTax = estimatedTax,
            estimatedTotal = estimatedTotal,
            itemCount = items.sumOf { it.quantity } + customSaleItems.size,
        )
    }
}
