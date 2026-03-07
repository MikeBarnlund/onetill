package com.onetill.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Auto-dismiss — duration varies by severity
    LaunchedEffect(toast) {
        if (toast != null) {
            val durationMs = when (toast.type) {
                ToastType.Success -> 3000L
                ToastType.Warning -> 5000L
                ToastType.Error -> 8000L
            }
            delay(durationMs)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(200, easing = FastOutSlowInEasing),
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(200, easing = FastOutSlowInEasing),
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.md, vertical = dimens.xs),
    ) {
        if (toast != null) {
            val iconColor = when (toast.type) {
                ToastType.Success -> colors.success
                ToastType.Warning -> colors.warning
                ToastType.Error -> colors.error
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.dismiss() }
                    .heightIn(min = 44.dp)
                    .background(
                        colors.surface.copy(alpha = 0.95f),
                        RoundedCornerShape(dimens.inputRadius),
                    )
                    .padding(horizontal = dimens.lg, vertical = dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.sm),
            ) {
                // Semantic icon — Canvas-drawn
                when (toast.type) {
                    ToastType.Success -> ToastCheckIcon(
                        color = iconColor,
                        modifier = Modifier.size(14.dp),
                    )
                    ToastType.Warning -> ToastWarningIcon(
                        color = iconColor,
                        modifier = Modifier.size(14.dp),
                    )
                    ToastType.Error -> ToastErrorIcon(
                        color = iconColor,
                        modifier = Modifier.size(14.dp),
                    )
                }

                // Message text — bodySmall per spec
                Text(
                    text = toast.message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

// Canvas-drawn toast icons

@Composable
private fun ToastCheckIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.8.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.5f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.8f, h * 0.28f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun ToastWarningIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        // Triangle outline
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.9f, h * 0.85f)
            lineTo(w * 0.1f, h * 0.85f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = s,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
        // Exclamation mark — vertical line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.38f),
            end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.58f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Exclamation mark — dot
        drawCircle(
            color = color,
            radius = s * 0.6f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.72f),
        )
    }
}

@Composable
private fun ToastErrorIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.8.dp.toPx()
        val pad = size.width * 0.2f
        // X mark
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(pad, pad),
            end = androidx.compose.ui.geometry.Offset(size.width - pad, size.height - pad),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width - pad, pad),
            end = androidx.compose.ui.geometry.Offset(pad, size.height - pad),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
