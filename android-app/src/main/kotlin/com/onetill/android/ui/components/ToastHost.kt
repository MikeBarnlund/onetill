package com.onetill.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme
import kotlinx.coroutines.delay

enum class ToastType {
    Success,
    Warning,
    Error,
}

data class ToastData(
    val message: String,
    val type: ToastType,
)

@Stable
class ToastState {
    var currentToast: ToastData? by mutableStateOf(null)
        private set

    fun show(message: String, type: ToastType = ToastType.Success) {
        currentToast = ToastData(message, type)
    }

    fun dismiss() {
        currentToast = null
    }
}

@Composable
fun ToastHost(
    state: ToastState,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val toast = state.currentToast

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2000)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.lg, vertical = dimens.sm),
    ) {
        if (toast != null) {
            val icon = when (toast.type) {
                ToastType.Success -> "\u2713"
                ToastType.Warning -> "\u26A0"
                ToastType.Error -> "\u2717"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(
                        colors.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(dimens.inputRadius),
                    )
                    .padding(horizontal = dimens.lg, vertical = dimens.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.sm),
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (toast.type) {
                        ToastType.Success -> colors.success
                        ToastType.Warning -> colors.warning
                        ToastType.Error -> colors.error
                    },
                )
                Text(
                    text = toast.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
        }
    }
}
