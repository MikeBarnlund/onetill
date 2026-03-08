package com.onetill.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun QrCodeIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val pad = w * 0.12f
        val cornerLen = w * 0.25f

        // Outer rounded rect
        drawRoundRect(
            color = color,
            topLeft = Offset(pad, pad),
            size = Size(w - 2 * pad, h - 2 * pad),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = s),
        )

        // Inner QR pattern — 3 squares
        val boxSize = w * 0.18f
        val inset = w * 0.22f
        // Top-left
        drawRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(boxSize, boxSize),
        )
        // Top-right
        drawRect(
            color = color,
            topLeft = Offset(w - inset - boxSize, inset),
            size = Size(boxSize, boxSize),
        )
        // Bottom-left
        drawRect(
            color = color,
            topLeft = Offset(inset, h - inset - boxSize),
            size = Size(boxSize, boxSize),
        )
        // Center dot
        val dotSize = w * 0.1f
        drawRect(
            color = color,
            topLeft = Offset((w - dotSize) / 2f, (h - dotSize) / 2f),
            size = Size(dotSize, dotSize),
        )
    }
}

@Composable
fun EditIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height

        // Pencil body (diagonal line)
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.8f),
            end = Offset(w * 0.75f, h * 0.25f),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
        // Pencil tip
        drawLine(
            color = color,
            start = Offset(w * 0.75f, h * 0.25f),
            end = Offset(w * 0.8f, h * 0.2f),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
        // Base line
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.85f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ScanFrameOverlay(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val s = 3.dp.toPx()
        val w = size.width
        val h = size.height
        val cornerLen = w * 0.12f

        val stroke = Stroke(width = s, cap = StrokeCap.Round)

        // Top-left corner
        drawLine(color, Offset(0f, s / 2), Offset(cornerLen, s / 2), s, cap = StrokeCap.Round)
        drawLine(color, Offset(s / 2, 0f), Offset(s / 2, cornerLen), s, cap = StrokeCap.Round)

        // Top-right corner
        drawLine(color, Offset(w - cornerLen, s / 2), Offset(w, s / 2), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w - s / 2, 0f), Offset(w - s / 2, cornerLen), s, cap = StrokeCap.Round)

        // Bottom-left corner
        drawLine(color, Offset(0f, h - s / 2), Offset(cornerLen, h - s / 2), s, cap = StrokeCap.Round)
        drawLine(color, Offset(s / 2, h - cornerLen), Offset(s / 2, h), s, cap = StrokeCap.Round)

        // Bottom-right corner
        drawLine(color, Offset(w - cornerLen, h - s / 2), Offset(w, h - s / 2), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w - s / 2, h - cornerLen), Offset(w - s / 2, h), s, cap = StrokeCap.Round)
    }
}

@Composable
fun ChevronIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height

        // Right-pointing chevron (rotate via graphicsLayer to point down)
        drawLine(
            color = color,
            start = Offset(w * 0.3f, h * 0.2f),
            end = Offset(w * 0.7f, h * 0.5f),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.7f, h * 0.5f),
            end = Offset(w * 0.3f, h * 0.8f),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
    }
}
