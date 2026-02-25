package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.CustomerDraft
import com.onetill.shared.ecommerce.woocommerce.dto.WooBillingDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCustomerDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomerMapperTest {

    @Test
    fun fullCustomerMapsCorrectly() {
        val dto = WooCustomerDto(
            id = 10,
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            billing = WooBillingDto(phone = "555-0100"),
        )
        val customer = dto.toDomain()
        assertEquals(10, customer.id)
        assertEquals("Jane", customer.firstName)
        assertEquals("Doe", customer.lastName)
        assertEquals("jane@example.com", customer.email)
        assertEquals("555-0100", customer.phone)
    }

    @Test
    fun emptyEmailMapsToNull() {
        val dto = WooCustomerDto(id = 1, firstName = "A", lastName = "B", email = "")
        assertNull(dto.toDomain().email)
    }

    @Test
    fun billingNullMeansPhoneNull() {
        val dto = WooCustomerDto(id = 1, firstName = "A", lastName = "B", billing = null)
        assertNull(dto.toDomain().phone)
    }

    @Test
    fun emptyPhoneMapsToNull() {
        val dto = WooCustomerDto(id = 1, firstName = "A", lastName = "B", billing = WooBillingDto(phone = ""))
        assertNull(dto.toDomain().phone)
    }

    @Test
    fun draftToWooDtoNullEmailAndPhone() {
        val draft = CustomerDraft(firstName = "John", lastName = "Smith", email = null, phone = null)
        val woo = draft.toWooDto()
        assertEquals("", woo.email)
        assertNull(woo.billing)
    }

    @Test
    fun draftToWooDtoWithPhone() {
        val draft = CustomerDraft(firstName = "John", lastName = "Smith", email = "j@x.com", phone = "555-1234")
        val woo = draft.toWooDto()
        assertEquals("j@x.com", woo.email)
        assertEquals("555-1234", woo.billing?.phone)
    }
}
