package com.onetill.android.ui.checkout

import androidx.compose.animation.animateContentSize
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
import com.onetill.android.ui.components.ConnectivityState
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.PaymentMethodCard
import com.onetill.android.ui.components.StatusBar
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onCashPayment: () -> Unit,
    onCardPaymentComplete: (String) -> Unit,
    viewModel: CheckoutViewModel = viewModel(),
) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors
    val orderTotal by viewModel.orderTotal.collectAsState()
    val selectedMethod by viewModel.selectedPaymentMethod.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    var emailReceipt by remember { mutableStateOf("") }
    var orderSummaryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Status bar
        StatusBar(
            connectivityState = if (isOnline) ConnectivityState.Online else ConnectivityState.Offline,
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
                text = "Checkout",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(dimens.touchTargetPrimary))
        }

        // Payment methods
        Column(
            modifier = Modifier.padding(horizontal = dimens.lg),
            verticalArrangement = Arrangement.spacedBy(dimens.md),
        ) {
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
            )

            PaymentMethodCard(
                icon = "\uD83D\uDCB3",
                title = "Card Payment",
                subtitle = if (isOnline) "Tap, chip, or swipe" else "Requires internet",
                isSelected = selectedMethod == PaymentMethodUi.Card,
                onClick = {
                    viewModel.selectPaymentMethod(PaymentMethodUi.Card)
                    if (isOnline) {
                        onCardPaymentComplete(viewModel.submitCardPayment())
                    }
                },
            )

            PaymentMethodCard(
                icon = "\uD83D\uDCB5",
                title = "Cash Payment",
                subtitle = "Enter amount received",
                isSelected = selectedMethod == PaymentMethodUi.Cash,
                onClick = {
                    viewModel.selectPaymentMethod(PaymentMethodUi.Cash)
                    onCashPayment()
                },
            )
        }

        Spacer(modifier = Modifier.height(dimens.xl))

        // Customer section
        Column(
            modifier = Modifier.padding(horizontal = dimens.lg),
            verticalArrangement = Arrangement.spacedBy(dimens.sm),
        ) {
            Text(
                text = "Customer (optional)",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "\u2713 Guest checkout",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }

        Spacer(modifier = Modifier.height(dimens.xl))

        // Email receipt
        Column(
            modifier = Modifier.padding(horizontal = dimens.lg),
            verticalArrangement = Arrangement.spacedBy(dimens.sm),
        ) {
            Text(
                text = "Send receipt? (optional)",
                style = MaterialTheme.typography.titleMedium,
            )
            OneTillTextField(
                value = emailReceipt,
                onValueChange = { emailReceipt = it },
                placeholder = "customer@email.com",
            )
        }

        Spacer(modifier = Modifier.height(dimens.xl))

        // Order summary (collapsible)
        Column(
            modifier = Modifier
                .padding(horizontal = dimens.lg)
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { orderSummaryExpanded = !orderSummaryExpanded }
                    .padding(vertical = dimens.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Order Summary (3 items)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row {
                    Text(
                        text = orderTotal,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (orderSummaryExpanded) " \u25B2" else " \u25BC",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (orderSummaryExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.sm)) {
                    OrderSummaryRow("Organic Honey 500g x2", "$29.98")
                    OrderSummaryRow("Sourdough Bread Loaf", "$8.99")
                    OrderSummaryRow("Artisan Cheese Wheel", "$24.99")
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.xxl))
    }
}

@Composable
private fun OrderSummaryRow(name: String, price: String) {
    val colors = OneTillTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Text(
            text = price,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
