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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundConfirmationSheet(
    order: OrderUiModel,
    refundState: RefundUiState,
    onDismiss: () -> Unit,
    onConfirmRefund: (restock: Boolean) -> Unit,
) {
    val colors = OneTillTheme.colors
    var restockItems by remember { mutableStateOf(true) }
    val isProcessing = refundState is RefundUiState.Processing

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismiss() },
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
            // Header
            Text(
                text = "Refund Order ${order.orderNumber}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Refund summary rows
            SummaryRow(label = "Refund amount", value = order.totalFormatted)
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(label = "Payment method", value = order.paymentMethod)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = colors.border)
            Spacer(modifier = Modifier.height(12.dp))

            // Restock toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Restock items",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${order.itemCount} ${if (order.itemCount == 1) "item" else "items"} will be restocked",
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                    )
                }
                Switch(
                    checked = restockItems,
                    onCheckedChange = { restockItems = it },
                    enabled = !isProcessing,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.textPrimary,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surface,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Warning text
            Text(
                text = "This will refund the full amount to the customer\u2019s original payment method. This action cannot be undone.",
                fontSize = 12.sp,
                color = colors.warning,
            )

            // Error message (inline, above buttons)
            if (refundState is RefundUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = refundState.message,
                    fontSize = 12.sp,
                    color = colors.error,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Cancel
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!isProcessing) colors.textSecondary else colors.textTertiary,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.textPrimary,
                        disabledContentColor = colors.textTertiary,
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Refund
                Button(
                    onClick = { onConfirmRefund(restockItems) },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.textPrimary,
                        disabledContainerColor = colors.error.copy(alpha = 0.6f),
                        disabledContentColor = colors.textPrimary.copy(alpha = 0.6f),
                    ),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = colors.textPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "Refund ${order.totalFormatted}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val colors = OneTillTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}
