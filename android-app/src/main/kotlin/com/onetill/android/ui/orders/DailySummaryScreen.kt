package com.onetill.android.ui.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.ChipVariant
import org.koin.androidx.compose.koinViewModel
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.components.StatusChip
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground

@Composable
fun DailySummaryScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val summary by viewModel.dailySummary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        // Status bar
        AppStatusBar()

        // Header — Back + "Today's Summary"
        ScreenHeader(
            title = "Today's Summary",
            navAction = HeaderNavAction.Back,
            onNavAction = onBack,
        )

        // Content — metric rows with dividers
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.md),
        ) {
            // Hero metric — Total Sales
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
            ) {
                Text(
                    text = "TOTAL SALES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textTertiary,
                    letterSpacing = 0.6.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary.totalSalesFormatted,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            // Transactions group
            SummaryRow(label = "Transactions", value = summary.transactionCount.toString())
            SummaryRow(label = "Average Order", value = summary.averageOrderFormatted)

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            // Payment breakdown
            SummaryRow(label = "Card Payments", value = summary.cardPaymentsFormatted)
            SummaryRow(label = "Cash Payments", value = summary.cashPaymentsFormatted)

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            // Items sold
            SummaryRow(label = "Items Sold", value = summary.itemsSold.toString())

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            // Pending sync notice
            if (summary.pendingSyncCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                StatusChip(
                    text = "Pending Sync: ${summary.pendingSyncCount} orders",
                    variant = ChipVariant.Warning,
                    icon = "⏳",
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val colors = OneTillTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}
