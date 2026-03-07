package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

private val NUM_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(".", "0", "⌫"),
)

@Composable
fun NumberPad(
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val keyShape = RoundedCornerShape(10.dp)

    // Backspace background — error at 15% opacity
    val backspaceBg = colors.error.copy(alpha = 0.15f)

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NUM_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    val isBackspace = key == "⌫"
                    val bg = if (isBackspace) backspaceBg else colors.surface
                    val textColor = if (isBackspace) colors.error else colors.textPrimary
                    val fontSize = if (isBackspace) 18.sp else 20.sp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(keyShape)
                            .border(1.dp, colors.border, keyShape)
                            .background(bg)
                            .clickable {
                                when (key) {
                                    "." -> onDot()
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key[0])
                                }
                            }
                            .semantics {
                                contentDescription = if (isBackspace) "Backspace" else key
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}
