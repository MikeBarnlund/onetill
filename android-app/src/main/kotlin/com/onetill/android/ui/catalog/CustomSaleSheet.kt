package com.onetill.android.ui.catalog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.NumberPad
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun CustomSaleSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (description: String, amountCents: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val sheetTopDp = 82.dp
    val sheetTopPx = with(LocalDensity.current) { sheetTopDp.roundToPx() }
    val sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    // Reset state when sheet opens
    LaunchedEffect(visible) {
        if (visible) {
            description = ""
            amountText = ""
        }
    }

    val amountCents = parseAmountCents(amountText)
    val canAdd = amountCents > 0

    val offsetAnimatable = remember { Animatable(1f) }

    LaunchedEffect(visible) {
        offsetAnimatable.animateTo(
            targetValue = if (visible) 0f else 1f,
            animationSpec = tween(
                durationMillis = if (visible) 250 else 200,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    val animatedOffset = offsetAnimatable.value

    if (animatedOffset >= 1f && !visible) return

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * (1f - animatedOffset)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // Bottom sheet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = sheetTopDp)
                .offset {
                    IntOffset(
                        0,
                        (animatedOffset * (1920 - sheetTopPx)).toInt(),
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(sheetShape)
                    .background(colors.background),
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 36.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                colors.textTertiary.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }

                // Title
                Text(
                    text = "Custom Sale",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description field
                OneTillTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Description (optional)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "AMOUNT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textTertiary,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = formatAmountDisplay(amountText),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                    }
                }

                // Number Pad
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = dimens.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    NumberPad(
                        onDigit = { digit ->
                            amountText = appendDigit(amountText, digit)
                        },
                        onDot = {
                            if (!amountText.contains('.')) {
                                amountText = if (amountText.isEmpty()) "0." else "$amountText."
                            }
                        },
                        onBackspace = {
                            if (amountText.isNotEmpty()) {
                                amountText = amountText.dropLast(1)
                            }
                        },
                    )
                }

                // Add to Cart button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
                ) {
                    OneTillButton(
                        text = "Add to Cart",
                        onClick = {
                            onAddToCart(description, amountCents)
                        },
                        enabled = canAdd,
                    )
                }
            }
        }
    }
}

private fun parseAmountCents(text: String): Long {
    if (text.isEmpty()) return 0
    val value = text.toDoubleOrNull() ?: return 0
    return (value * 100).toLong()
}

private fun formatAmountDisplay(text: String): String {
    if (text.isEmpty()) return "$0.00"
    val value = text.toDoubleOrNull() ?: return "$$text"
    return if (text.contains('.')) {
        val parts = text.split('.')
        val decPart = parts.getOrElse(1) { "" }
        when (decPart.length) {
            0 -> "$$text"
            1 -> "$$text"
            else -> "$${String.format("%.2f", value)}"
        }
    } else {
        "$$text"
    }
}

private fun appendDigit(current: String, digit: Char): String {
    if (current.contains('.')) {
        val decPart = current.substringAfter('.')
        if (decPart.length >= 2) return current
    }
    if (current == "0" && digit != '.') return digit.toString()
    return current + digit
}
