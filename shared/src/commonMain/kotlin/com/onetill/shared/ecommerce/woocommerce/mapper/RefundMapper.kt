package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.Refund
import com.onetill.shared.data.model.toMoney
import com.onetill.shared.ecommerce.woocommerce.dto.WooRefundDto

fun WooRefundDto.toDomain(orderId: Long, currency: String): Refund = Refund(
    id = id,
    orderId = orderId,
    amount = amount.toMoney(currency),
    reason = reason.ifEmpty { null },
    createdAt = parseWooDateTime(dateCreated),
)
