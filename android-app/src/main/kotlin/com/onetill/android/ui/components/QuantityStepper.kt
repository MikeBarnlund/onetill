package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = OneTillTheme.dimens
    val shape = RoundedCornerShape(dimens.inputRadius)
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .semantics { contentDescription = "Quantity: $quantity" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Minus button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (quantity > 1) MaterialTheme.colorScheme.surfaceVariant
                    else OneTillTheme.colors.disabledContainer,
                )
                .then(
                    if (quantity > 1) {
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
            Text(
                text = "\u2212",
                style = MaterialTheme.typography.titleMedium,
                color = if (quantity > 1) MaterialTheme.colorScheme.onSurface
                else OneTillTheme.colors.disabled,
            )
        }

        // Count display
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }

        // Plus button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
