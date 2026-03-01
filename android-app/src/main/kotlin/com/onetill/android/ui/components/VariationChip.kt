package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun VariationChip(
    label: String,
    isSelected: Boolean,
    isOutOfStock: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors
    val shape = RoundedCornerShape(dimens.inputRadius)

    val bgColor = when {
        isOutOfStock -> colors.surface
        isSelected -> colors.accent
        else -> colors.surface
    }
    val textColor = when {
        isOutOfStock -> colors.textTertiary
        isSelected -> colors.textPrimary
        else -> colors.textPrimary
    }
    val borderMod = if (!isSelected && !isOutOfStock) {
        Modifier.border(1.dp, colors.border, shape)
    } else Modifier

    Box(
        modifier = modifier
            .height(44.dp)
            .widthIn(min = 60.dp)
            .clip(shape)
            .then(borderMod)
            .background(bgColor)
            .clickable(enabled = !isOutOfStock, onClick = onClick)
            .padding(horizontal = dimens.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            textDecoration = if (isOutOfStock) TextDecoration.LineThrough else null,
        )
    }
}
