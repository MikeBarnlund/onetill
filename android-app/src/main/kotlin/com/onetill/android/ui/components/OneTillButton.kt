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
import androidx.compose.ui.unit.dp
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
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors
    val shape = RoundedCornerShape(dimens.buttonRadius)

    when (variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.buttonHeightPrimary),
                enabled = enabled,
                shape = shape,
                contentPadding = PaddingValues(horizontal = dimens.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = colors.disabledContainer,
                    disabledContentColor = colors.disabled,
                ),
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }

        ButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = dimens.buttonHeightSecondary),
                enabled = enabled,
                shape = shape,
                contentPadding = PaddingValues(horizontal = dimens.lg),
                border = BorderStroke(
                    1.dp,
                    if (enabled) MaterialTheme.colorScheme.primary else colors.disabled,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = colors.disabled,
                ),
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }

        ButtonVariant.Destructive -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = dimens.buttonHeightSecondary),
                enabled = enabled,
                shape = shape,
                contentPadding = PaddingValues(horizontal = dimens.lg),
                border = BorderStroke(
                    1.dp,
                    if (enabled) MaterialTheme.colorScheme.error else colors.disabled,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContentColor = colors.disabled,
                ),
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }

        ButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = dimens.buttonHeightSecondary),
                enabled = enabled,
                shape = shape,
                contentPadding = PaddingValues(horizontal = dimens.lg),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = colors.disabled,
                ),
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
