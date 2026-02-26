package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors

    val connectivityColor = when (connectivityState) {
        ConnectivityState.Online -> colors.success
        ConnectivityState.Offline -> colors.warning
        ConnectivityState.Syncing -> MaterialTheme.colorScheme.primary
    }
    val connectivityLabel = when (connectivityState) {
        ConnectivityState.Online -> "Online"
        ConnectivityState.Offline -> "Offline"
        ConnectivityState.Syncing -> "Syncing"
    }

    val batteryColor = when {
        batteryPercent < 10 -> colors.error
        batteryPercent < 20 -> colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.statusBarHeight)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = dimens.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Connectivity
            Row(
                modifier = Modifier
                    .sizeIn(minWidth = dimens.touchTargetPrimary, minHeight = dimens.touchTargetPrimary)
                    .clip(CircleShape)
                    .clickable(onClick = onConnectivityTap)
                    .semantics { contentDescription = "Connection: $connectivityLabel" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(connectivityColor, CircleShape),
                )
                Text(
                    text = connectivityLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Center-left: Sync status
            Text(
                text = syncStatusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Center-right: Battery
            Text(
                text = "\uD83D\uDD0B $batteryPercent%",
                style = MaterialTheme.typography.labelMedium,
                color = batteryColor,
            )

            // Right: Time
            Text(
                text = currentTime,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
