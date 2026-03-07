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
    onRemove: (() -> Unit)? = null,
    maxQuantity: Int? = null,
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
        // Minus / Trash button
        val isTrash = quantity == 1 && onRemove != null
        val minusEnabled = quantity > 1 || isTrash
        Box(
            modifier = Modifier
                .size(width = dimens.stepperWidth, height = dimens.stepperHeight)
                .background(colors.surface)
                .then(
                    if (isTrash) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onRemove!!() })
                        }
                    } else if (minusEnabled) {
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
            if (isTrash) {
                StepperTrashIcon(
                    color = colors.error,
                    modifier = Modifier.size(dimens.stepperIconSize),
                )
            } else {
                StepperMinusIcon(
                    color = if (minusEnabled) colors.textSecondary else colors.textTertiary,
                    modifier = Modifier.size(dimens.stepperIconSize),
                )
            }
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
        val plusEnabled = maxQuantity == null || quantity < maxQuantity
        Box(
            modifier = Modifier
                .size(width = dimens.stepperWidth, height = dimens.stepperHeight)
                .background(colors.surface)
                .then(
                    if (plusEnabled) {
                        Modifier.pointerInput(quantity, maxQuantity) {
                            detectTapGestures(
                                onTap = { onQuantityChange(quantity + 1) },
                                onLongPress = {
                                    scope.launch {
                                        delay(250)
                                        var current = quantity
                                        val limit = maxQuantity ?: Int.MAX_VALUE
                                        while (current < limit) {
                                            current++
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
            StepperPlusIcon(
                color = if (plusEnabled) colors.textSecondary else colors.textTertiary,
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

@Composable
private fun StepperTrashIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        // Lid line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.22f),
            end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.22f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Handle nub
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.22f),
            end = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.14f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.14f),
            end = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.14f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.14f),
            end = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.22f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Body (tapered trapezoid)
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.88f)
            lineTo(w * 0.70f, h * 0.88f)
            lineTo(w * 0.75f, h * 0.28f)
            close()
        }
        drawPath(path = body, color = color, style = stroke)
        // Inner vertical lines
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.36f),
            end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.78f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
