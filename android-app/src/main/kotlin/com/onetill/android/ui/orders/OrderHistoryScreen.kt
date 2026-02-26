package com.onetill.android.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.ChipVariant
import com.onetill.android.ui.components.ConnectivityState
import com.onetill.android.ui.components.StatusBar
import com.onetill.android.ui.components.StatusChip
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = viewModel(),
) {
    val dimens = OneTillTheme.dimens
    val orders by viewModel.orders.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status bar
        StatusBar(
            connectivityState = ConnectivityState.Online,
            syncStatusText = "Synced",
            batteryPercent = 85,
            currentTime = "3:42 PM",
        )

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.screenHeaderHeight)
                .padding(horizontal = dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(dimens.touchTargetPrimary),
            ) {
                Text(text = "\u2190", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                text = "Orders",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )

            // Filter dropdown
            TextButton(onClick = { showFilterMenu = true }) {
                Text(
                    text = "${selectedFilter.name} \u25BC",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false },
            ) {
                OrderFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.name) },
                        onClick = {
                            viewModel.setFilter(filter)
                            showFilterMenu = false
                        },
                    )
                }
            }
        }

        // Order list
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = orders, key = { it.id }) { order ->
                OrderRow(order = order)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dimens.lg),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderUiModel) {
    val dimens = OneTillTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .padding(horizontal = dimens.lg, vertical = dimens.md),
        verticalArrangement = Arrangement.spacedBy(dimens.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Order ${order.orderNumber}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = order.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${order.itemCount} items \u2022 ${order.paymentMethod}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = order.totalFormatted,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        StatusChip(
            text = when (order.syncStatus) {
                SyncStatus.Synced -> "\u2713 Synced"
                SyncStatus.Pending -> "\u23F3 Pending sync"
                SyncStatus.Failed -> "\u2717 Failed"
            },
            variant = when (order.syncStatus) {
                SyncStatus.Synced -> ChipVariant.Success
                SyncStatus.Pending -> ChipVariant.Warning
                SyncStatus.Failed -> ChipVariant.Error
            },
        )
    }
}
