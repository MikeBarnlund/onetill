package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun NumberPad(
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = OneTillTheme.dimens
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "\u232B"),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.sm),
            ) {
                row.forEach { key ->
                    NumberPadKey(
                        label = key,
                        onClick = {
                            when (key) {
                                "." -> onDot()
                                "\u232B" -> onBackspace()
                                else -> onDigit(key[0])
                            }
                        },
                        isBackspace = key == "\u232B",
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPadKey(
    label: String,
    onClick: () -> Unit,
    isBackspace: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OneTillTheme.dimens.inputRadius)
    val colors = OneTillTheme.colors
    val bgColor = if (isBackspace) {
        colors.surface
    } else {
        colors.surface
    }

    Box(
        modifier = modifier
            .width(72.dp)
            .height(56.dp)
            .clip(shape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isBackspace) "Backspace" else label
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
    }
}
