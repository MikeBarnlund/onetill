package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.ecommerce.woocommerce.dto.WooCouponLineDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooMetaDataDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooOrderDto
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderMapperTest {

    @Test
    fun customerIdZeroMapsToNull() {
        val dto = WooOrderDto(id = 1, customerId = 0)
        assertNull(dto.toDomain("USD").customerId)
    }

    @Test
    fun customerIdPositiveMapsAsIs() {
        val dto = WooOrderDto(id = 1, customerId = 42)
        assertEquals(42L, dto.toDomain("USD").customerId)
    }

    @Test
    fun statusMappingAllVariants() {
        fun map(s: String) = WooOrderDto(id = 1, status = s).toDomain("USD").status
        assertEquals(OrderStatus.PENDING, map("pending"))
        assertEquals(OrderStatus.PROCESSING, map("processing"))
        assertEquals(OrderStatus.COMPLETED, map("completed"))
        assertEquals(OrderStatus.CANCELLED, map("cancelled"))
        assertEquals(OrderStatus.REFUNDED, map("refunded"))
        assertEquals(OrderStatus.FAILED, map("failed"))
        assertEquals(OrderStatus.PENDING, map("on-hold")) // unknown → PENDING
    }

    @Test
    fun paymentMethodMapping() {
        fun map(m: String) = WooOrderDto(id = 1, paymentMethod = m).toDomain("USD").paymentMethod
        assertEquals(PaymentMethod.CARD, map("stripe"))
        assertEquals(PaymentMethod.CARD, map("stripe_terminal"))
        assertEquals(PaymentMethod.CARD, map("credit_card"))
        assertEquals(PaymentMethod.CASH, map("cash"))
        assertEquals(PaymentMethod.CASH, map("bacs")) // unknown → CASH
    }

    @Test
    fun stripeTransactionIdExtractedFromMetaData() {
        val dto = WooOrderDto(
            id = 1,
            metaData = listOf(
                WooMetaDataDto(key = "_onetill_stripe_id", value = JsonPrimitive("pi_abc123")),
            ),
        )
        assertEquals("pi_abc123", dto.toDomain("USD").stripeTransactionId)
    }

    @Test
    fun couponCodesMappedFromCouponLines() {
        val dto = WooOrderDto(
            id = 1,
            couponLines = listOf(
                WooCouponLineDto(code = "SAVE10"),
                WooCouponLineDto(code = "WELCOME"),
            ),
        )
        assertEquals(listOf("SAVE10", "WELCOME"), dto.toDomain("USD").couponCodes)
    }

    @Test
    fun draftToWooDtoCardVsCash() {
        val lineItem = LineItem(null, 1, null, "Widget", "SKU", 2, Money(1000, "USD"), Money(2000, "USD"))

        val cardDraft = OrderDraft(listOf(lineItem), null, PaymentMethod.CARD, "key1", null, listOf("SAVE10"))
        val cardWoo = cardDraft.toWooDto("USD")
        assertEquals("stripe", cardWoo.paymentMethod)
        assertEquals(false, cardWoo.setPaid)
        assertEquals(1, cardWoo.couponLines.size)
        assertEquals("SAVE10", cardWoo.couponLines[0].code)

        val cashDraft = OrderDraft(listOf(lineItem), null, PaymentMethod.CASH, "key2", "A note", emptyList())
        val cashWoo = cashDraft.toWooDto("USD")
        assertEquals("cash", cashWoo.paymentMethod)
        assertEquals(true, cashWoo.setPaid)
        assertTrue(cashWoo.metaData.any { it.key == "_onetill_note" && it.value == "A note" })
    }
}
