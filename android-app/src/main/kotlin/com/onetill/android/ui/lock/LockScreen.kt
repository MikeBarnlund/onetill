package com.onetill.android.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.R
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground

@Composable
fun LockScreen(
    visible: Boolean,
    onPinEntered: (String) -> Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(300, easing = FastOutSlowInEasing)),
    ) {
        LockScreenContent(onPinEntered = onPinEntered)
    }
}

@Composable
private fun LockScreenContent(
    onPinEntered: (String) -> Boolean,
) {
    val colors = OneTillTheme.colors

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.onetill_logo),
                contentDescription = "OneTill",
                modifier = Modifier.size(64.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (index < pin.length) {
                                    if (error) colors.error else colors.accent
                                } else {
                                    colors.surface
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error / hint text
            Text(
                text = if (error) "Incorrect PIN" else "Enter PIN to unlock",
                fontSize = 13.sp,
                color = if (error) colors.error else colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Number pad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫"),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        for (key in row) {
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                PinKey(
                                    label = key,
                                    onClick = {
                                        error = false
                                        if (key == "⌫") {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                            }
                                        } else if (pin.length < 4) {
                                            pin += key
                                            if (pin.length == 4) {
                                                val valid = onPinEntered(pin)
                                                if (!valid) {
                                                    error = true
                                                    pin = ""
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    label: String,
    onClick: () -> Unit,
) {
    val colors = OneTillTheme.colors

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(colors.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = if (label == "⌫") "Delete" else label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = if (label == "⌫") 20.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
        )
    }
}
