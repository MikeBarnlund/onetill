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
                val resolvedStockOrNull = variant.resolvedStockQuantity(this@toPickerProduct)
                val tracksStock = resolvedStockOrNull != null
                val resolvedStock = resolvedStockOrNull ?: 0
                PickerOption(
                    label = attr.value,
                    // Untracked stock = always sellable; tracked = needs > 0 on hand.
                    available = !tracksStock || resolvedStock > 0,
                    priceAdjustment = priceAdjustment,
                    stockCount = resolvedStock,
                    tracksStock = tracksStock,
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

    // Default to the first sellable variant (untracked stock or qty > 0) so the
    // picker doesn't open on a sold-out combination. Falls back to the first
    // variant only when every variant is out of stock.
    val firstSellable = variants.firstOrNull { v ->
        val stock = v.resolvedStockQuantity(this@toPickerProduct)
        stock == null || stock > 0
    }
    val defaultVariant = firstSellable ?: variants.firstOrNull()

    return PickerProduct(
        name = name,
        imageUrl = images.firstOrNull()?.url,
        startingPriceFormatted = formatCents(baseCents),
        attributes = attributeGroups,
        resolvedPriceFormatted = formatCents(
            defaultVariant?.price?.amountCents ?: baseCents,
        ),
        stockCount = defaultVariant?.resolvedStockQuantity(this@toPickerProduct) ?: 0,
        variants = pickerVariants,
        defaultSelections = defaultVariant?.attributes
            ?.associate { it.name to it.value }
            ?: emptyMap(),
    )
}
