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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme

enum class ConnectivityState {
    Online,
    Offline,
    Syncing,
}

@Composable
fun StatusBar(
    connectivityState: ConnectivityState,
    syncStatusText: String,
    batteryPercent: Int,
    currentTime: String,
    modifier: Modifier = Modifier,
    onConnectivityTap: () -> Unit = {},
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val micro = OneTillTheme.extraTypography.micro

    val connectivityColor = when (connectivityState) {
        ConnectivityState.Online -> colors.success
        ConnectivityState.Offline -> colors.warning
        ConnectivityState.Syncing -> colors.accent
    }
    val connectivityLabel = when (connectivityState) {
        ConnectivityState.Online -> "Online"
        ConnectivityState.Offline -> "Offline"
        ConnectivityState.Syncing -> "Syncing"
    }

    val syncColor = when {
        syncStatusText.contains("failed", ignoreCase = true) -> colors.error
        syncStatusText.contains("pending", ignoreCase = true) -> colors.warning
        else -> colors.textTertiary
    }

    val batteryColor = when {
        batteryPercent < 10 -> colors.error
        batteryPercent < 20 -> colors.warning
        else -> colors.textTertiary
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.statusBarHeight)
                .padding(horizontal = dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Connectivity dot + label
            Row(
                modifier = Modifier
                    .sizeIn(
                        minWidth = dimens.touchTargetPrimary,
                        minHeight = dimens.statusBarHeight,
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onConnectivityTap)
                    .semantics { contentDescription = "Connection: $connectivityLabel" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Box(
                    modifier = Modifier
                        .size(dimens.connectivityDotSize)
                        .background(connectivityColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(dimens.xs))
                Text(
                    text = connectivityLabel,
                    style = micro,
                    color = colors.textTertiary,
                )
            }

            // Center-left: Sync status
            Text(
                text = syncStatusText,
                style = micro,
                color = syncColor,
            )

            // Center-right: Battery
            Row(verticalAlignment = Alignment.CenterVertically) {
                BatteryIcon(
                    percent = batteryPercent,
                    color = batteryColor,
                    modifier = Modifier.size(width = 16.dp, height = 10.dp),
                )
                Spacer(modifier = Modifier.width(dimens.xs))
                Text(
                    text = "$batteryPercent%",
                    style = micro,
                    color = colors.textTertiary,
                )
            }

            // Right: Time
            Text(
                text = currentTime,
                style = micro,
                color = colors.textTertiary,
            )
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = colors.borderSubtle,
        )
    }
}

@Composable
private fun BatteryIcon(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val tipWidth = w * 0.08f
        val bodyWidth = w - tipWidth
        val cornerRadius = h * 0.15f
        val fillFraction = (percent / 100f).coerceIn(0f, 1f)

        // Battery outline
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset.Zero,
            size = androidx.compose.ui.geometry.Size(bodyWidth, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )
        // Battery tip
        val tipH = h * 0.4f
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(bodyWidth, (h - tipH) / 2f),
            size = androidx.compose.ui.geometry.Size(tipWidth, tipH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * 0.5f),
        )
        // Fill level
        val inset = 2.dp.toPx()
        val fillWidth = (bodyWidth - inset * 2) * fillFraction
        if (fillWidth > 0f) {
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(fillWidth, h - inset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * 0.5f),
            )
        }
    }
}
