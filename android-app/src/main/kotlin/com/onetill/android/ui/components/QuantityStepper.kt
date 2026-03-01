package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val shape = RoundedCornerShape(dimens.stepperRadius)
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .semantics { contentDescription = "Quantity: $quantity" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Minus button
        val minusEnabled = quantity > 1
        Box(
            modifier = Modifier
                .size(width = dimens.stepperWidth, height = dimens.stepperHeight)
                .background(colors.surface)
                .then(
                    if (minusEnabled) {
                        Modifier.pointerInput(quantity) {
                            detectTapGestures(
                                onTap = { onQuantityChange(quantity - 1) },
                                onLongPress = {
                                    scope.launch {
                                        delay(250)
                                        var current = quantity
                                        while (current > 1) {
                                            current--
                                            onQuantityChange(current)
                                            delay(100)
                                        }
                                    }
                                },
                            )
                        }
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            StepperMinusIcon(
                color = if (minusEnabled) colors.textSecondary else colors.textTertiary,
                modifier = Modifier.size(dimens.stepperIconSize),
            )
        }

        // Count display
        Box(
            modifier = Modifier
                .size(width = dimens.stepperWidth, height = dimens.stepperHeight)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = quantity.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        }

        // Plus button
        Box(
            modifier = Modifier
                .size(width = dimens.stepperWidth, height = dimens.stepperHeight)
                .background(colors.surface)
                .pointerInput(quantity) {
                    detectTapGestures(
                        onTap = { onQuantityChange(quantity + 1) },
                        onLongPress = {
                            scope.launch {
                                delay(250)
                                var current = quantity
                                while (true) {
                                    current++
                                    onQuantityChange(current)
                                    delay(100)
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            StepperPlusIcon(
                color = colors.textSecondary,
                modifier = Modifier.size(dimens.stepperIconSize),
            )
        }
    }
}

@Composable
private fun StepperMinusIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.5.dp.toPx()
        val pad = size.width * 0.25f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(pad, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width - pad, size.height / 2f),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun StepperPlusIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.5.dp.toPx()
        val pad = size.width * 0.25f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(pad, cy),
            end = androidx.compose.ui.geometry.Offset(size.width - pad, cy),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx, pad),
            end = androidx.compose.ui.geometry.Offset(cx, size.height - pad),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
