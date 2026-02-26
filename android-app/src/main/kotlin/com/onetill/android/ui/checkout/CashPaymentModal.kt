package com.onetill.android.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.BottomActionBar
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.NumberPad
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.theme.OneTillTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashPaymentModal(
    onClose: () -> Unit,
    onPaymentComplete: (String) -> Unit,
    viewModel: CheckoutViewModel = viewModel(),
) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors
    val orderTotal by viewModel.orderTotal.collectAsState()
    val orderTotalCents by viewModel.orderTotalCents.collectAsState()
    var amountText by remember { mutableStateOf("") }

    val amountCents = parseAmountCents(amountText)
    val changeCents = if (amountCents >= orderTotalCents) amountCents - orderTotalCents else 0L
    val canComplete = amountCents >= orderTotalCents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.sm, vertical = dimens.sm),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(dimens.touchTargetPrimary),
            ) {
                Text(text = "\u2715", style = MaterialTheme.typography.headlineMedium)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Total due
            Text(
                text = "Total Due",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = orderTotal,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(vertical = dimens.sm),
            )

            Spacer(modifier = Modifier.height(dimens.xl))

            // Amount received display
            Text(
                text = "Amount Received",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (amountText.isEmpty()) "$0.00" else formatInput(amountText),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(vertical = dimens.sm),
            )

            Spacer(modifier = Modifier.height(dimens.lg))

            // Number pad
            NumberPad(
                onDigit = { amountText += it },
                onDot = { if (!amountText.contains('.')) amountText += '.' },
                onBackspace = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) },
            )

            Spacer(modifier = Modifier.height(dimens.lg))

            // Quick amounts
            Text(
                text = "Quick amounts:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(dimens.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                verticalArrangement = Arrangement.spacedBy(dimens.sm),
            ) {
                OneTillButton(
                    text = "Exact",
                    onClick = { amountText = formatExact(orderTotalCents) },
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                quickAmounts(orderTotalCents).forEach { quickCents ->
                    OneTillButton(
                        text = formatCentsShort(quickCents),
                        onClick = { amountText = formatExact(quickCents) },
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.lg))

            // Change due
            if (canComplete) {
                Text(
                    text = "Change due: ${formatCentsDisplay(changeCents)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.success,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(dimens.lg))
        }

        // Complete sale button
        BottomActionBar {
            OneTillButton(
                text = "Complete Sale",
                onClick = { onPaymentComplete(orderTotal) },
                enabled = canComplete,
            )
        }
    }
}

private fun parseAmountCents(text: String): Long {
    if (text.isEmpty()) return 0
    val value = text.toDoubleOrNull() ?: return 0
    return (value * 100).toLong()
}

private fun formatInput(text: String): String {
    val value = text.toDoubleOrNull() ?: return "$$text"
    return "$${String.format("%.2f", value)}"
}

private fun formatExact(cents: Long): String {
    val d = cents / 100
    val r = cents % 100
    return "$d.${r.toString().padStart(2, '0')}"
}

private fun formatCentsDisplay(cents: Long): String {
    val d = cents / 100
    val r = cents % 100
    return "$${d}.${r.toString().padStart(2, '0')}"
}

private fun formatCentsShort(cents: Long): String {
    val d = cents / 100
    return "$$d"
}

private fun quickAmounts(totalCents: Long): List<Long> {
    val bills = listOf(500L, 1000L, 2000L, 5000L, 10000L)
    return bills.filter { it > totalCents }.take(2)
}
