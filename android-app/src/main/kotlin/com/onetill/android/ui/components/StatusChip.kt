package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
}

@Composable
fun StatusChip(
    text: String,
    variant: ChipVariant,
    modifier: Modifier = Modifier,
    icon: String? = null,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val micro = OneTillTheme.extraTypography.micro

    val (bgColor, textColor) = when (variant) {
        ChipVariant.Success -> colors.successContainer to colors.success
        ChipVariant.Warning -> colors.warningContainer to colors.warning
        ChipVariant.Error -> colors.errorContainer to colors.error
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(dimens.chipRadius))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (icon != null) {
            Text(
                text = icon,
                style = micro,
                color = textColor,
            )
        }
        Text(
            text = text,
            style = micro,
            color = textColor,
        )
    }
}
