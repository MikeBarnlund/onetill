package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme

enum class HeaderNavAction {
    Back,
    Close,
    Menu,
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    navAction: HeaderNavAction? = null,
    onNavAction: () -> Unit = {},
    rightActions: @Composable () -> Unit = {},
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.screenHeaderHeight)
            .padding(horizontal = dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: nav action button or empty spacer
        if (navAction != null) {
            HeaderActionButton(
                navAction = navAction,
                onClick = onNavAction,
            )
        } else {
            Spacer(modifier = Modifier.size(dimens.headerActionSize))
        }

        // Center: title
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        // Right: action slot or empty spacer
        Box(contentAlignment = Alignment.CenterEnd) {
            rightActions()
        }
    }
}

@Composable
fun HeaderActionButton(
    navAction: HeaderNavAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val label = when (navAction) {
        HeaderNavAction.Back -> "Go back"
        HeaderNavAction.Close -> "Close"
        HeaderNavAction.Menu -> "Open menu"
    }

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = dimens.touchTargetPrimary,
                minHeight = dimens.touchTargetPrimary,
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dimens.headerActionSize)
                .background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (navAction) {
                HeaderNavAction.Back -> BackArrowIcon(
                    color = colors.textPrimary,
                    modifier = Modifier.size(dimens.headerIconSize),
                )
                HeaderNavAction.Close -> CloseIcon(
                    color = colors.textPrimary,
                    modifier = Modifier.size(dimens.headerIconSize),
                )
                HeaderNavAction.Menu -> MenuIcon(
                    color = colors.textPrimary,
                    modifier = Modifier.size(dimens.headerIconSize),
                )
            }
        }
    }
}

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dimens = OneTillTheme.dimens

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = dimens.touchTargetSecondary,
                minHeight = dimens.touchTargetSecondary,
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// Vector icons drawn with Canvas — no Material Icons dependency

@Composable
fun BackArrowIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.8.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val armLen = size.width * 0.35f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx + armLen * 0.2f, cy - armLen)
            lineTo(cx - armLen * 0.6f, cy)
            lineTo(cx + armLen * 0.2f, cy + armLen)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        // Horizontal line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx - armLen * 0.6f, cy),
            end = androidx.compose.ui.geometry.Offset(cx + armLen * 0.8f, cy),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.8.dp.toPx()
        val pad = size.width * 0.25f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(pad, pad),
            end = androidx.compose.ui.geometry.Offset(size.width - pad, size.height - pad),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width - pad, pad),
            end = androidx.compose.ui.geometry.Offset(pad, size.height - pad),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
fun MenuIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.8.dp.toPx()
        val padH = size.width * 0.15f
        val gaps = size.height / 4f
        for (i in 1..3) {
            val y = gaps * i
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(padH, y),
                end = androidx.compose.ui.geometry.Offset(size.width - padH, y),
                strokeWidth = strokeW,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.8.dp.toPx()
        val cx = size.width * 0.42f
        val cy = size.height * 0.42f
        val r = size.width * 0.3f
        drawCircle(
            color = color,
            radius = r,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW),
        )
        // Handle
        val handleStart = r * 0.7f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx + handleStart, cy + handleStart),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
fun BarcodeIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        // Outer rectangle
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.09f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(w * 0.82f, h * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW),
        )
        // Barcode lines
        val lines = listOf(0.27f, 0.41f, 0.52f, 0.64f, 0.75f)
        for (xFrac in lines) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * xFrac, h * 0.34f),
                end = androidx.compose.ui.geometry.Offset(w * xFrac, h * 0.66f),
                strokeWidth = strokeW,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}
