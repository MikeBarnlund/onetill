package com.onetill.android.ui.orders

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.ChipVariant
import org.koin.androidx.compose.koinViewModel
import com.onetill.android.ui.components.ChevronDownIcon
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.components.StatusChip
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradient

@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val orders by viewModel.orders.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val expandedOrderId by viewModel.expandedOrderId.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
    ) {
        // Status bar
        AppStatusBar()

        // Header — Back + "Orders" + filter dropdown
        ScreenHeader(
            title = "Orders",
            navAction = HeaderNavAction.Back,
            onNavAction = onBack,
            rightActions = {
                Row(
                    modifier = Modifier.clickable { showFilterMenu = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedFilter.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.accentLight,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ChevronDownIcon(
                        color = colors.accentLight,
                        modifier = Modifier.size(12.dp),
                    )
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                ) {
                    OrderFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = filter.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                )
                            },
                            onClick = {
                                viewModel.setFilter(filter)
                                showFilterMenu = false
                            },
                        )
                    }
                }
            },
        )

        // Order list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.md),
        ) {
            itemsIndexed(items = orders, key = { _, order -> order.id }) { index, order ->
                OrderRow(
                    order = order,
                    isExpanded = expandedOrderId == order.id,
                    onClick = { viewModel.toggleOrderExpanded(order.id) },
                )
                if (index < orders.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                }
            }
        }
    }
}

@Composable
private fun OrderRow(
    order: OrderUiModel,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = OneTillTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing,
                ),
            )
            .padding(vertical = 14.dp),
    ) {
        // Row 1: Order ID + Status chip + Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Order ${order.orderNumber}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                StatusChip(
                    text = when (order.syncStatus) {
                        SyncStatus.Synced -> "Synced"
                        SyncStatus.Pending -> "Pending sync"
                        SyncStatus.Failed -> "Failed"
                    },
                    variant = when (order.syncStatus) {
                        SyncStatus.Synced -> ChipVariant.Success
                        SyncStatus.Pending -> ChipVariant.Warning
                        SyncStatus.Failed -> ChipVariant.Error
                    },
                    icon = when (order.syncStatus) {
                        SyncStatus.Synced -> "✓"
                        SyncStatus.Pending -> "⏳"
                        SyncStatus.Failed -> "✕"
                    },
                )
            }
            Text(
                text = order.time,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Row 2: Items + method + Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${order.itemCount} ${if (order.itemCount == 1) "item" else "items"} · ${order.paymentMethod}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )
            Text(
                text = order.totalFormatted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        }

        // Expanded: line item details
        if (isExpanded && order.lineItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = colors.border)
            Spacer(modifier = Modifier.height(8.dp))

            order.lineItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${item.quantity}× ${item.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = item.totalFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}
