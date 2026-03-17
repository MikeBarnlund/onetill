package com.onetill.android.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.ChipVariant
import com.onetill.android.ui.components.StatusChip
import com.onetill.android.ui.theme.OneTillTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailSheet(
    order: OrderUiModel,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onRefundClick: () -> Unit,
) {
    val colors = OneTillTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        colors.textPrimary.copy(alpha = 0.5f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            // Header: Order number + status chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Order ${order.orderNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                StatusChip(
                    text = when (order.syncStatus) {
                        SyncStatus.Synced -> "Completed"
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
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time + payment method
            Text(
                text = "${order.time} · ${order.paymentMethod}",
                fontSize = 12.sp,
                color = colors.textTertiary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = colors.border)
            Spacer(modifier = Modifier.height(12.dp))

            // Line items
            order.lineItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${item.quantity}x ${item.name}",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.totalFormatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = colors.border)
            Spacer(modifier = Modifier.height(12.dp))

            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Subtotal",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
                Text(
                    text = order.subtotalFormatted,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
            }

            // Tax
            if (order.hasTax) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Tax",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = order.taxFormatted,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Total",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Text(
                    text = order.totalFormatted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Refund button — only for eligible orders
            if (order.isEligibleForRefund) {
                Button(
                    onClick = onRefundClick,
                    enabled = isOnline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.textPrimary,
                        disabledContainerColor = colors.surface,
                        disabledContentColor = colors.textTertiary,
                    ),
                ) {
                    Text(
                        text = "Refund Order",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Disabled reason
                if (!isOnline) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Refunds require internet",
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
