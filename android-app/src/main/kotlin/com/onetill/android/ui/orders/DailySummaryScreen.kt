package com.onetill.android.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.ConnectivityState
import com.onetill.android.ui.components.StatusBar
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun DailySummaryScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = viewModel(),
) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors
    val summary by viewModel.dailySummary.collectAsState()

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
                text = "Today's Summary",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(dimens.touchTargetPrimary))
        }

        // Summary content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.lg, vertical = dimens.xl),
            verticalArrangement = Arrangement.spacedBy(dimens.md),
        ) {
            // Total sales (hero)
            Text(
                text = "Total Sales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = summary.totalSalesFormatted,
                style = MaterialTheme.typography.displayMedium,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            SummaryRow("Transactions", summary.transactionCount.toString())
            SummaryRow("Average Order", summary.averageOrderFormatted)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            SummaryRow("Card Payments", summary.cardPaymentsFormatted)
            SummaryRow("Cash Payments", summary.cashPaymentsFormatted)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            SummaryRow("Items Sold", summary.itemsSold.toString())

            if (summary.pendingSyncCount > 0) {
                Spacer(modifier = Modifier.height(dimens.md))
                Text(
                    text = "Pending Sync: ${summary.pendingSyncCount} orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.warning,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
