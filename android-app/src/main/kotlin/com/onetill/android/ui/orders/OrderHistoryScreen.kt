package com.onetill.android.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.onetill.android.ui.components.ToastHost
import com.onetill.android.ui.components.ToastState
import com.onetill.android.ui.components.ToastType
import com.onetill.shared.orders.OrderFilter
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground

@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val orders by viewModel.orders.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val showRefundConfirmation by viewModel.showRefundConfirmation.collectAsState()
    val refundState by viewModel.refundState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }

    val toastState = remember { ToastState() }

    // Show toast on refund success
    LaunchedEffect(refundState) {
        if (refundState is RefundUiState.Success) {
            val orderNumber = (refundState as RefundUiState.Success).orderNumber
            toastState.show("Order $orderNumber refunded", ToastType.Success)
            viewModel.dismissRefundResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenGradientBackground(),
        ) {
            // Status bar
            AppStatusBar()

            // Header
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
                        onClick = { viewModel.selectOrder(order) },
                    )
                    if (index < orders.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = colors.border)
                    }
                }
            }
        }

        // Toast overlay
        ToastHost(
            state = toastState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    // Order detail bottom sheet
    if (selectedOrder != null) {
        OrderDetailSheet(
            order = selectedOrder!!,
            isOnline = isOnline,
            onDismiss = { viewModel.dismissOrderDetail() },
            onRefundClick = { viewModel.showRefundConfirmation() },
        )
    }

    // Refund confirmation bottom sheet
    if (showRefundConfirmation && selectedOrder != null) {
        RefundConfirmationSheet(
            order = selectedOrder!!,
            refundState = refundState,
            onDismiss = { viewModel.dismissRefundConfirmation() },
            onConfirmRefund = { restock -> viewModel.initiateRefund(restock) },
        )
    }
}

@Composable
private fun OrderRow(
    order: OrderUiModel,
    onClick: () -> Unit,
) {
    val colors = OneTillTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        SyncStatus.ForwardingFailed -> "Payment declined"
                        SyncStatus.Refunded -> "Refunded"
                    },
                    variant = when (order.syncStatus) {
                        SyncStatus.Synced -> ChipVariant.Success
                        SyncStatus.Pending -> ChipVariant.Warning
                        SyncStatus.Failed -> ChipVariant.Error
                        SyncStatus.ForwardingFailed -> ChipVariant.Error
                        SyncStatus.Refunded -> ChipVariant.Error
                    },
                    icon = when (order.syncStatus) {
                        SyncStatus.Synced -> "\u2713"
                        SyncStatus.Pending -> "\u23F3"
                        SyncStatus.Failed -> "\u2715"
                        SyncStatus.ForwardingFailed -> "\u2715"
                        SyncStatus.Refunded -> "\u21A9"
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
                text = "${order.itemCount} ${if (order.itemCount == 1) "item" else "items"} \u00B7 ${order.paymentMethod}",
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
    }
}
