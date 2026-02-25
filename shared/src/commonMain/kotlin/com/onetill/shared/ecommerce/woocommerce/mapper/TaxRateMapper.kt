package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.TaxRate
import com.onetill.shared.ecommerce.woocommerce.dto.WooTaxRateDto

fun WooTaxRateDto.toDomain(): TaxRate = TaxRate(
    id = id,
    name = name,
    rate = rate,
    country = country,
    state = state,
    isCompound = compound,
    isShipping = shipping,
)
