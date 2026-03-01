package com.onetill.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

enum class ButtonVariant {
    Primary,
    Secondary,
    Destructive,
    Ghost,
}

@Composable
fun OneTillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    when (variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.buttonHeightPrimary),
                enabled = enabled,
                shape = RoundedCornerShape(dimens.buttonRadiusPrimary),
                contentPadding = PaddingValues(horizontal = dimens.lg),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textPrimary,
                    disabledContainerColor = colors.surface,
                    disabledContentColor = colors.textTertiary,
                ),
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        ButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = dimens.buttonHeightSecondary),
                enabled = enabled,
                shape = RoundedCornerShape(dimens.buttonRadiusSecondary),
                contentPadding = PaddingValues(horizontal = dimens.lg),
                border = BorderStroke(
                    1.dp,
                    if (enabled) colors.accent else colors.textTertiary,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.surface,
                    contentColor = colors.accent,
                    disabledContentColor = colors.textTertiary,
                ),
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        ButtonVariant.Destructive -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = dimens.touchTargetSecondary),
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = dimens.sm),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.error,
                    disabledContentColor = colors.textTertiary,
                ),
            ) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        ButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = dimens.touchTargetSecondary),
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = dimens.sm),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.accent,
                    disabledContentColor = colors.textTertiary,
                ),
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
