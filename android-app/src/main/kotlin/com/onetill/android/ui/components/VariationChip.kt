package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun VariationChip(
    label: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    priceAdjustment: String? = null,
    stockCount: Int = 0,
    tracksStock: Boolean = true,
) {
    val colors = OneTillTheme.colors
    val shape = RoundedCornerShape(10.dp)

    val bgColor = if (isSelected) colors.accent else colors.surface
    val borderColor = if (isSelected) colors.accent else colors.border
    val textColor = if (isSelected) colors.textOnAccent else colors.textPrimary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
    val textDecoration = if (!isAvailable) TextDecoration.LineThrough else TextDecoration.None

    val priceColor = if (isSelected) {
        colors.textOnAccent.copy(alpha = 0.6f)
    } else {
        colors.textTertiary
    }

    // Stock indicator: only shown when the variant tracks inventory. Surfaces the
    // count so it's clear why a sold-out option can't be selected.
    val stockLabel = when {
        !tracksStock -> null
        stockCount <= 0 -> "Sold out"
        else -> "$stockCount left"
    }
    val stockColor = when {
        stockCount <= 0 -> colors.error
        isSelected -> colors.textOnAccent.copy(alpha = 0.7f)
        else -> colors.textTertiary
    }

    Column(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(bgColor)
            .alpha(if (isAvailable) 1f else 0.4f)
            .then(
                if (isAvailable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics {
                contentDescription = buildString {
                    append(label)
                    if (priceAdjustment != null) append(" $priceAdjustment")
                    if (stockLabel != null) append(", $stockLabel")
                    if (isSelected) append(", selected")
                }
            },
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = fontWeight,
                color = textColor,
                textDecoration = textDecoration,
            )

            // Price adjustment suffix (e.g. "+$50")
            if (priceAdjustment != null && isAvailable) {
                Text(
                    text = priceAdjustment,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = priceColor,
                )
            }
        }

        if (stockLabel != null) {
            Text(
                text = stockLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = stockColor,
            )
        }
    }
}
