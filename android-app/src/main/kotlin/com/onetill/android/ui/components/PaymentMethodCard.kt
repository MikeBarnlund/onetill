package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun PaymentMethodCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color? = null,
) {
    val colors = OneTillTheme.colors
    val shape = RoundedCornerShape(12.dp)

    val bgColor = if (isSelected) colors.accentMuted else colors.surface
    val borderColor = if (isSelected) colors.accent else colors.border
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = "$title — $subtitle" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon circle — 40×40dp, rgba(255,255,255,0.05) bg, 10dp radius
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleColor ?: colors.textTertiary,
            )
        }

        // Chevron right
        ChevronRightIcon(
            color = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun CardPaymentIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        // Card rectangle with rounded corners
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.083f, h * 0.208f),
            size = androidx.compose.ui.geometry.Size(w * 0.833f, h * 0.583f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.104f),
            style = stroke,
        )
        // Magnetic stripe line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.083f, h * 0.417f),
            end = androidx.compose.ui.geometry.Offset(w * 0.917f, h * 0.417f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Bottom-left detail line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.604f),
            end = androidx.compose.ui.geometry.Offset(w * 0.417f, h * 0.604f),
            strokeWidth = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
fun CashPaymentIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        // Bill rectangle
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.042f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.917f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.083f),
            style = stroke,
        )
        // Center circle
        drawCircle(
            color = color,
            radius = w * 0.125f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )
        // Top-left dot
        drawCircle(
            color = color,
            radius = w * 0.02f,
            center = androidx.compose.ui.geometry.Offset(w * 0.208f, h * 0.375f),
        )
        // Bottom-right dot
        drawCircle(
            color = color,
            radius = w * 0.02f,
            center = androidx.compose.ui.geometry.Offset(w * 0.792f, h * 0.625f),
        )
    }
}

@Composable
fun ChevronRightIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.389f, h * 0.222f)
            lineTo(w * 0.667f, h * 0.5f)
            lineTo(w * 0.389f, h * 0.778f)
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
fun MailIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = s,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        // Envelope rectangle
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.083f, h * 0.194f),
            size = androidx.compose.ui.geometry.Size(w * 0.833f, h * 0.611f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.111f),
            style = stroke,
        )
        // V-flap
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.083f, h * 0.306f)
            lineTo(w * 0.5f, h * 0.583f)
            lineTo(w * 0.917f, h * 0.306f)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
fun CheckmarkIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.8.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.143f, h * 0.5f)
            lineTo(w * 0.393f, h * 0.75f)
            lineTo(w * 0.857f, h * 0.25f)
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
fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.214f, h * 0.357f)
            lineTo(w * 0.5f, h * 0.643f)
            lineTo(w * 0.786f, h * 0.357f)
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
fun ChevronUpIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.214f, h * 0.643f)
            lineTo(w * 0.5f, h * 0.357f)
            lineTo(w * 0.786f, h * 0.643f)
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
