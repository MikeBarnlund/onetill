package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme

enum class ChipVariant {
    Success,
    Warning,
    Error,
    Neutral,
}

@Composable
fun StatusChip(
    text: String,
    variant: ChipVariant,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors

    val (bgColor, textColor) = when (variant) {
        ChipVariant.Success -> colors.success.copy(alpha = 0.15f) to colors.success
        ChipVariant.Warning -> colors.warning.copy(alpha = 0.15f) to colors.warning
        ChipVariant.Error -> colors.error.copy(alpha = 0.15f) to colors.error
        ChipVariant.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .height(24.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}
