package com.onetill.android.ui.components

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.stripe.StripeTerminalManager
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.sync.ConnectivityMonitor
import com.onetill.shared.sync.SyncOrchestrator
import com.onetill.shared.sync.SyncStatus
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnectivityState {
    Online,
    Offline,
    Syncing,
}

@Composable
fun AppStatusBar(
    modifier: Modifier = Modifier,
    connectivityMonitor: ConnectivityMonitor = koinInject(),
) {
    val syncOrchestrator = remember {
        KoinPlatform.getKoin().getOrNull<SyncOrchestrator>()
    }

    val syncStatus by syncOrchestrator?.syncStatus?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(SyncStatus.Idle) }
    val pendingOrderCount by syncOrchestrator?.pendingOrderCount?.collectAsState(initial = 0L)
        ?: remember { androidx.compose.runtime.mutableStateOf(0L) }
    val isOnline by connectivityMonitor.isOnline.collectAsState()

    val localDataSource = remember { KoinPlatform.getKoin().getOrNull<LocalDataSource>() }
    val offlinePaymentsEnabled by localDataSource?.observeOfflinePaymentEnabled()?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }
    val stripeTerminalManager = remember { KoinPlatform.getKoin().getOrNull<StripeTerminalManager>() }
    val offlineStatus by stripeTerminalManager?.offlineStatus?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val connectivityState = if (isOnline) ConnectivityState.Online else ConnectivityState.Offline

    val pendingOfflineCount = offlineStatus?.sdk?.offlinePaymentsCount ?: 0

    val syncStatusText = when {
        syncStatus is SyncStatus.Syncing -> "Syncing..."
        syncStatus is SyncStatus.Error -> "Sync failed"
        pendingOrderCount > 0 -> "Pending sync ($pendingOrderCount)"
        else -> "Synced"
    }

    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }

    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var battery by remember { mutableStateOf(getBatteryState(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            currentTime = timeFormat.format(Date())
            battery = getBatteryState(context)
        }
    }

    val connectivityLabel = when {
        isOnline -> null
        pendingOfflineCount > 0 -> "Offline · $pendingOfflineCount pending"
        else -> null
    }

    StatusBar(
        connectivityState = connectivityState,
        syncStatusText = syncStatusText,
        batteryPercent = battery.percent,
        isCharging = battery.isCharging,
        currentTime = currentTime,
        connectivityLabelOverride = connectivityLabel,
        modifier = modifier,
    )
}

private data class BatteryState(val percent: Int, val isCharging: Boolean)

private fun getBatteryState(context: Context): BatteryState {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
    return BatteryState(percent, isCharging)
}

@Composable
fun StatusBar(
    connectivityState: ConnectivityState,
    syncStatusText: String,
    batteryPercent: Int,
    isCharging: Boolean = false,
    currentTime: String,
    connectivityLabelOverride: String? = null,
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
    val connectivityLabel = connectivityLabelOverride ?: when (connectivityState) {
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
        isCharging -> colors.success
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
                    text = if (isCharging) "$batteryPercent% ⚡" else "$batteryPercent%",
                    style = micro,
                    color = batteryColor,
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
