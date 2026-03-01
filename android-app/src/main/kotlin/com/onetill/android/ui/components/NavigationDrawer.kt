package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun NavigationDrawer(
    onOrdersTap: () -> Unit,
    onSummaryTap: () -> Unit,
    onSettingsTap: () -> Unit,
    modifier: Modifier = Modifier,
    versionText: String = "v1.0.0 · Register 1",
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Column(
        modifier = modifier
            .width(dimens.drawerWidth)
            .fillMaxHeight()
            .background(colors.drawer),
    ) {
        // Logo + brand
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Logo placeholder — 36×36dp circle with accent
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.accent, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "O",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "OneTill",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                letterSpacing = (-0.01).sp,
            )
        }

        // Divider
        HorizontalDivider(
            thickness = 1.dp,
            color = colors.borderSubtle,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // Nav items section
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            DrawerNavItem(
                label = "Orders",
                icon = { color -> OrdersIcon(color = color, modifier = Modifier.size(dimens.headerIconSize)) },
                onClick = onOrdersTap,
            )
            DrawerNavItem(
                label = "Summary",
                icon = { color -> SummaryIcon(color = color, modifier = Modifier.size(dimens.headerIconSize)) },
                onClick = onSummaryTap,
            )
        }

        // Flex spacer
        Spacer(modifier = Modifier.weight(1f))

        // Settings pinned above footer
        DrawerNavItem(
            label = "Settings",
            icon = { color -> SettingsIcon(color = color, modifier = Modifier.size(dimens.headerIconSize)) },
            onClick = onSettingsTap,
        )

        // Footer
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = colors.borderSubtle,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = versionText,
                fontSize = 11.sp,
                color = colors.textTertiary,
            )
        }
    }
}

// Drawer icons drawn with Canvas

@Composable
fun OrdersIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Clipboard outline
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = s),
        )
        // Top tab
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = s),
        )
        // Lines
        for (frac in listOf(0.35f, 0.5f, 0.65f)) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * 0.3f, h * frac),
                end = androidx.compose.ui.geometry.Offset(w * 0.7f, h * frac),
                strokeWidth = s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
fun SummaryIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Rounded rect
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = s),
        )
        // Lines
        drawLine(
            color = color, strokeWidth = s, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            start = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.4f),
            end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.4f),
        )
        drawLine(
            color = color, strokeWidth = s, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            start = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.6f),
            end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.6f),
        )
    }
}

@Composable
fun SettingsIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.2f
        // Center circle
        drawCircle(
            color = color,
            radius = r,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = s),
        )
        // Gear teeth as short lines radiating out
        val outerR = size.width * 0.4f
        val innerR = size.width * 0.28f
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0))
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(cx + innerR * cos, cy + innerR * sin),
                end = androidx.compose.ui.geometry.Offset(cx + outerR * cos, cy + outerR * sin),
                strokeWidth = s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}
