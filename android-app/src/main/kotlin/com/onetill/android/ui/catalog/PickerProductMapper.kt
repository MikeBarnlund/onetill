package com.onetill.android.ui.catalog

import com.onetill.shared.data.model.Product
import com.onetill.shared.util.formatCents

fun Product.toPickerProduct(): PickerProduct {
    val baseCents = price.amountCents

    val attributeGroups = variants
        .flatMap { it.attributes }
        .groupBy { it.name }
        .map { (attrName, attrs) ->
            val seenValues = mutableSetOf<String>()
            val options = attrs.mapNotNull { attr ->
                if (!seenValues.add(attr.value)) return@mapNotNull null
                val variant = variants.first { v ->
                    v.attributes.any { it.name == attrName && it.value == attr.value }
                }
                val variantCents = variant.price.amountCents
                val delta = variantCents - baseCents
                val priceAdjustment = when {
                    delta > 0 -> "+${formatCents(delta)}"
                    delta < 0 -> "-${formatCents(-delta)}"
                    else -> null
                }
                PickerOption(
                    label = attr.value,
                    available = (variant.stockQuantity ?: 0) > 0,
                    priceAdjustment = priceAdjustment,
                    stockCount = variant.stockQuantity ?: 0,
                )
            }
            PickerAttributeGroup(name = attrName, options = options)
        }

    val pickerVariants = variants.map { v ->
        PickerVariant(
            attributes = v.attributes.associate { it.name to it.value },
            priceFormatted = formatCents(v.price.amountCents),
            stockCount = v.stockQuantity ?: 0,
            manageStock = v.manageStock,
        )
    }

    val firstAvailable = variants.firstOrNull { (it.stockQuantity ?: 0) > 0 }

    return PickerProduct(
        name = name,
        imageUrl = images.firstOrNull()?.url,
        startingPriceFormatted = formatCents(baseCents),
        attributes = attributeGroups,
        resolvedPriceFormatted = formatCents(
            firstAvailable?.price?.amountCents ?: baseCents,
        ),
        stockCount = firstAvailable?.stockQuantity ?: 0,
        variants = pickerVariants,
    )
}
