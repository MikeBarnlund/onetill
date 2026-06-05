package com.onetill.android.ui.catalog

import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.resolvedImageUrl
import com.onetill.shared.data.model.resolvedRegularPrice
import com.onetill.shared.data.model.resolvedSalePrice
import com.onetill.shared.data.model.resolvedStockQuantity
import com.onetill.shared.util.formatCents
import com.onetill.shared.util.formatDisplay

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
                val resolvedStock = variant.resolvedStockQuantity(this@toPickerProduct) ?: 0
                PickerOption(
                    label = attr.value,
                    available = resolvedStock > 0,
                    priceAdjustment = priceAdjustment,
                    stockCount = resolvedStock,
                )
            }
            PickerAttributeGroup(name = attrName, options = options)
        }

    val pickerVariants = variants.map { v ->
        PickerVariant(
            attributes = v.attributes.associate { it.name to it.value },
            imageUrl = v.resolvedImageUrl(this@toPickerProduct),
            priceFormatted = formatCents(v.price.amountCents),
            regularPriceFormatted = v.resolvedRegularPrice(this@toPickerProduct)?.formatDisplay(),
            salePriceFormatted = v.resolvedSalePrice(this@toPickerProduct)?.formatDisplay(),
            stockCount = v.resolvedStockQuantity(this@toPickerProduct) ?: 0,
            manageStock = v.manageStock,
        )
    }

    val firstAvailable = variants.firstOrNull {
        (it.resolvedStockQuantity(this@toPickerProduct) ?: 0) > 0
    }

    return PickerProduct(
        name = name,
        imageUrl = images.firstOrNull()?.url,
        startingPriceFormatted = formatCents(baseCents),
        attributes = attributeGroups,
        resolvedPriceFormatted = formatCents(
            firstAvailable?.price?.amountCents ?: baseCents,
        ),
        stockCount = firstAvailable?.resolvedStockQuantity(this@toPickerProduct) ?: 0,
        variants = pickerVariants,
    )
}
