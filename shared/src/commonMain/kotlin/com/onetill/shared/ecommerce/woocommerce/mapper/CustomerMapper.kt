package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.Customer
import com.onetill.shared.data.model.CustomerDraft
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateBillingDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateCustomerDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCustomerDto

fun WooCustomerDto.toDomain(): Customer = Customer(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email.ifEmpty { null },
    phone = billing?.phone?.ifEmpty { null },
)

fun CustomerDraft.toWooDto(): WooCreateCustomerDto = WooCreateCustomerDto(
    firstName = firstName,
    lastName = lastName,
    email = email ?: "",
    billing = phone?.let { WooCreateBillingDto(phone = it) },
)
