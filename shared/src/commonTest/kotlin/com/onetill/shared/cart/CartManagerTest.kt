package com.onetill.shared.cart

import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.fake.FakeLocalDataSource
import com.onetill.shared.fake.testProduct
import com.onetill.shared.fake.testTaxRate
import com.onetill.shared.fake.testVariant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun applyCouponDeduplicatesCaseInsensitive() = runTest(testDispatcher) {
        val cart = createCartManager()

        cart.applyCoupon("save10")
        cart.applyCoupon("SAVE10")

        assertEquals(1, cart.cartState.value.couponCodes.size)
        assertEquals("SAVE10", cart.cartState.value.couponCodes[0])
    }

    @Test
    fun removeCoupon() = runTest(testDispatcher) {
        val cart = createCartManager()
        cart.applyCoupon("SAVE10")
        cart.applyCoupon("WELCOME")

        cart.removeCoupon("save10")

        assertEquals(listOf("WELCOME"), cart.cartState.value.couponCodes)
    }

    @Test
    fun setNoteBlankBecomesNull() = runTest(testDispatcher) {
        val cart = createCartManager()

        cart.setNote("   ")

        assertNull(cart.cartState.value.note)
    }

    @Test
    fun clearCartResetsEverything() = runTest(testDispatcher) {
        val cart = createCartManager()
        cart.addProduct(testProduct())
        cart.applyCoupon("SAVE10")
        cart.setCustomer(42)
        cart.setNote("test note")

        cart.clearCart()

        val state = cart.cartState.value
        assertTrue(state.isEmpty)
        assertTrue(state.couponCodes.isEmpty())
        assertNull(state.customerId)
        assertNull(state.note)
    }

    @Test
    fun buildOrderDraftCarriesAllFields() = runTest(testDispatcher) {
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
