package com.onetill.shared.cart

import com.onetill.shared.data.model.CouponType
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.fake.FakeLocalDataSource
import com.onetill.shared.fake.testCoupon
import com.onetill.shared.fake.testProduct
import com.onetill.shared.fake.testTaxRate
import com.onetill.shared.fake.testVariant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CartManagerTest {

    private val fakeLocal = FakeLocalDataSource()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createCartManager() = CartManager(
        localDataSource = fakeLocal,
        currency = "USD",
        scope = testScope,
    )

    @Test
    fun initialStateIsEmpty() = runTest(testDispatcher) {
        val cart = createCartManager()
        val state = cart.cartState.value
        assertTrue(state.isEmpty)
        assertEquals(0, state.itemCount)
        assertEquals(0L, state.subtotal.amountCents)
    }

    @Test
    fun addSimpleProduct() = runTest(testDispatcher) {
        val cart = createCartManager()
        val product = testProduct(price = 1999)

        cart.addProduct(product)

        val state = cart.cartState.value
        assertEquals(1, state.items.size)
        assertEquals(1, state.itemCount)
        assertEquals(1999L, state.subtotal.amountCents)
    }

    @Test
    fun addSameProductTwiceMergesQuantity() = runTest(testDispatcher) {
        val cart = createCartManager()
        val product = testProduct(id = 1, price = 1000)

        cart.addProduct(product)
        cart.addProduct(product)

        val state = cart.cartState.value
        assertEquals(1, state.items.size)
        assertEquals(2, state.items[0].quantity)
        assertEquals(2, state.itemCount)
        assertEquals(2000L, state.subtotal.amountCents)
    }

    @Test
    fun addProductWithVariantSeparateItems() = runTest(testDispatcher) {
        val cart = createCartManager()
        val product = testProduct(id = 1, price = 1000)
        val variant = testVariant(id = 100, productId = 1, price = 1500)

        cart.addProduct(product)
        cart.addProduct(product, variant)

        val state = cart.cartState.value
        assertEquals(2, state.items.size)
        assertEquals(2, state.itemCount)
        assertEquals(2500L, state.subtotal.amountCents)
    }

    @Test
    fun removeItem() = runTest(testDispatcher) {
        val cart = createCartManager()
        cart.addProduct(testProduct(id = 1, price = 500))
        cart.addProduct(testProduct(id = 2, name = "Other", price = 700))

        cart.removeItem(productId = 1)

        val state = cart.cartState.value
        assertEquals(1, state.items.size)
        assertEquals(2L, state.items[0].productId)
        assertEquals(700L, state.subtotal.amountCents)
    }

    @Test
    fun updateQuantity() = runTest(testDispatcher) {
        val cart = createCartManager()
        cart.addProduct(testProduct(id = 1, price = 1000))

        cart.updateQuantity(productId = 1, variantId = null, newQuantity = 3)

        val state = cart.cartState.value
        assertEquals(3, state.items[0].quantity)
        assertEquals(3, state.itemCount)
        assertEquals(3000L, state.subtotal.amountCents)
    }

    @Test
    fun updateQuantityToZeroRemovesItem() = runTest(testDispatcher) {
        val cart = createCartManager()
        cart.addProduct(testProduct(id = 1))

        cart.updateQuantity(productId = 1, variantId = null, newQuantity = 0)

        assertTrue(cart.cartState.value.isEmpty)
    }

    // -- Coupon validation --

    @Test
    fun applyCouponRejectsUnknownCode() = runTest(testDispatcher) {
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 10000))

        val result = cart.applyCoupon("BOGUS")

        assertIs<CouponApplyResult.Invalid>(result)
        assertEquals("Invalid coupon code", result.reason)
        assertTrue(cart.cartState.value.appliedCoupons.isEmpty())
    }

    @Test
    fun applyCouponAcceptsValidCode() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "SAVE10", type = CouponType.PERCENT, amount = "10.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 10000)) // $100

        val result = cart.applyCoupon("save10")

        assertIs<CouponApplyResult.Applied>(result)
        assertEquals("SAVE10", result.code)
        assertEquals(1000L, result.discountAmount.amountCents) // 10% of $100
    }

    @Test
    fun applyCouponDeduplicatesCaseInsensitive() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "SAVE10"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 10000))

        cart.applyCoupon("save10")
        val result = cart.applyCoupon("SAVE10")

        assertIs<CouponApplyResult.Invalid>(result)
        assertEquals("Coupon already applied", result.reason)
        assertEquals(1, cart.cartState.value.appliedCoupons.size)
    }

    @Test
    fun removeCoupon() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "SAVE10"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 10000))
        cart.applyCoupon("SAVE10")

        cart.removeCoupon("save10")

        assertTrue(cart.cartState.value.appliedCoupons.isEmpty())
        assertEquals(0L, cart.cartState.value.discountTotal.amountCents)
    }

    // -- Discount calculation --

    @Test
    fun percentCouponCalculatesCorrectDiscount() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "HALF", type = CouponType.PERCENT, amount = "50.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 2000)) // $20

        cart.applyCoupon("HALF")

        val state = cart.cartState.value
        assertEquals(1000L, state.discountTotal.amountCents) // $10
        assertEquals(1000L, state.estimatedTotal.amountCents) // $20 - $10 = $10
    }

    @Test
    fun fixedCartCouponCalculatesCorrectDiscount() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "FIVE", type = CouponType.FIXED_CART, amount = "5.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 2000)) // $20

        cart.applyCoupon("FIVE")

        val state = cart.cartState.value
        assertEquals(500L, state.discountTotal.amountCents) // $5
        assertEquals(1500L, state.estimatedTotal.amountCents) // $20 - $5 = $15
    }

    @Test
    fun fixedProductCouponScalesWithItemCount() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "BUCK", type = CouponType.FIXED_PRODUCT, amount = "1.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 2000)) // $20
        cart.updateQuantity(productId = 1, variantId = null, newQuantity = 3) // 3 x $20

        cart.applyCoupon("BUCK")

        val state = cart.cartState.value
        assertEquals(300L, state.discountTotal.amountCents) // $1 x 3 items
        assertEquals(5700L, state.estimatedTotal.amountCents) // $60 - $3 = $57
    }

    @Test
    fun discountCannotExceedSubtotal() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "BIG", type = CouponType.FIXED_CART, amount = "999.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 500)) // $5

        cart.applyCoupon("BIG")

        val state = cart.cartState.value
        assertEquals(500L, state.discountTotal.amountCents) // capped at $5
        assertEquals(0L, state.estimatedTotal.amountCents)
    }

    @Test
    fun percentDiscountRecalculatesWhenCartChanges() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "TEN", type = CouponType.PERCENT, amount = "10.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(id = 1, price = 10000)) // $100

        cart.applyCoupon("TEN")
        assertEquals(1000L, cart.cartState.value.discountTotal.amountCents) // $10

        // Add another item — discount should scale up
        cart.addProduct(testProduct(id = 2, name = "Other", price = 5000)) // +$50

        val state = cart.cartState.value
        assertEquals(15000L, state.subtotal.amountCents) // $150
        assertEquals(1500L, state.discountTotal.amountCents) // 10% of $150 = $15
        assertEquals(13500L, state.estimatedTotal.amountCents) // $150 - $15 = $135
    }

    @Test
    fun discountAppliedBeforeTax() = runTest(testDispatcher) {
        fakeLocal.taxRates.add(testTaxRate(rate = "10.0"))
        fakeLocal.coupons.add(testCoupon(code = "TEN", type = CouponType.FIXED_CART, amount = "10.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(price = 10000)) // $100

        cart.applyCoupon("TEN")

        val state = cart.cartState.value
        assertEquals(10000L, state.subtotal.amountCents) // $100
        assertEquals(1000L, state.discountTotal.amountCents) // $10
        // Tax is on discounted subtotal: ($100 - $10) * 10% = $9
        assertEquals(900L, state.estimatedTax.amountCents)
        assertEquals(9900L, state.estimatedTotal.amountCents) // $90 + $9 = $99
    }

    @Test
    fun couponCodesCarryThroughToOrderDraft() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "SALE"))
        val cart = createCartManager()
        cart.addProduct(testProduct(id = 5, price = 2000))
        cart.applyCoupon("SALE")

        val draft = cart.buildOrderDraft(PaymentMethod.CASH)

        assertEquals(listOf("SALE"), draft.couponCodes)
        assertTrue(draft.discountCents > 0)
    }

    // -- Other --

    @Test
    fun setNoteBlankBecomesNull() = runTest(testDispatcher) {
        val cart = createCartManager()

        cart.setNote("   ")

        assertNull(cart.cartState.value.note)
    }

    @Test
    fun clearCartResetsEverything() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "SAVE10"))
        val cart = createCartManager()
        cart.addProduct(testProduct())
        cart.applyCoupon("SAVE10")
        cart.setCustomer(42)
        cart.setNote("test note")

        cart.clearCart()

        val state = cart.cartState.value
        assertTrue(state.isEmpty)
        assertTrue(state.appliedCoupons.isEmpty())
        assertEquals(0L, state.discountTotal.amountCents)
        assertNull(state.customerId)
        assertNull(state.note)
    }

    @Test
    fun buildOrderDraftCarriesAllFields() = runTest(testDispatcher) {
        fakeLocal.coupons.add(testCoupon(code = "SALE", type = CouponType.PERCENT, amount = "10.00"))
        val cart = createCartManager()
        cart.addProduct(testProduct(id = 5, price = 2000))
        cart.applyCoupon("SALE")
        cart.setCustomer(99)
        cart.setNote("Gift wrap")

        val draft = cart.buildOrderDraft(PaymentMethod.CASH)

        assertEquals(1, draft.lineItems.size)
        assertEquals(5L, draft.lineItems[0].productId)
        assertEquals(2000L, draft.lineItems[0].totalPrice.amountCents)
        assertEquals(99L, draft.customerId)
        assertEquals(PaymentMethod.CASH, draft.paymentMethod)
        assertEquals(listOf("SALE"), draft.couponCodes)
        assertEquals(200L, draft.discountCents) // 10% of $20
        assertEquals("Gift wrap", draft.note)
        assertNotEquals("", draft.idempotencyKey)
    }

    @Test
    fun taxRatesAffectEstimatedTotal() = runTest(testDispatcher) {
        fakeLocal.taxRates.add(testTaxRate(rate = "10.0"))
        val cart = createCartManager()

        cart.addProduct(testProduct(price = 10000)) // $100

        val state = cart.cartState.value
        assertEquals(10000L, state.subtotal.amountCents)
        assertEquals(1000L, state.estimatedTax.amountCents)
        assertEquals(11000L, state.estimatedTotal.amountCents)
    }
}
