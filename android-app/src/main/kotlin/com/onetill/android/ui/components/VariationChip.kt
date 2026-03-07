package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
) {
    val colors = OneTillTheme.colors
    val shape = RoundedCornerShape(10.dp)

    val bgColor = if (isSelected) colors.accent else colors.surface
    val borderColor = if (isSelected) colors.accent else colors.border
    val textColor = if (isSelected) Color.White else colors.textPrimary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
    val textDecoration = if (!isAvailable) TextDecoration.LineThrough else TextDecoration.None

    val priceColor = if (isSelected) {
        Color.White.copy(alpha = 0.6f)
    } else {
        colors.textTertiary
    }

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(bgColor)
            .alpha(if (isAvailable) 1f else 0.35f)
            .then(
                if (isAvailable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp)
            .semantics {
                contentDescription = buildString {
                    append(label)
                    if (priceAdjustment != null) append(" $priceAdjustment")
                    if (!isAvailable) append(", out of stock")
                    if (isSelected) append(", selected")
                }
            },
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
}
