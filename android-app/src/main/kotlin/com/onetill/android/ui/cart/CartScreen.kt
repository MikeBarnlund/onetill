package com.onetill.android.ui.cart

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.BottomActionBar
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.CartLineItem
import com.onetill.android.ui.components.ConnectivityState
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.StatusBar
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = viewModel(),
) {
    val dimens = OneTillTheme.dimens
    val state by viewModel.cartState.collectAsState()
    var showCouponField by remember { mutableStateOf(false) }
    var couponInput by remember { mutableStateOf("") }

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
                text = "Cart (${state.itemCount} items)",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )

            TextButton(
                onClick = { viewModel.clearCart() },
                modifier = Modifier.size(width = 80.dp, height = dimens.touchTargetSecondary),
            ) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Line items
        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(items = state.items, key = { it.id }) { item ->
                CartLineItem(
                    name = item.name,
                    variationInfo = item.variationInfo,
                    imageUrl = item.imageUrl,
                    quantity = item.quantity,
                    lineTotalFormatted = CartViewModel.formatCents(item.totalPriceCents),
                    onQuantityChange = { viewModel.updateQuantity(item.id, it) },
                )
            }

            // Coupon row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.lg, vertical = dimens.md)
                        .animateContentSize(),
                ) {
                    if (state.couponCode != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "\uD83C\uDFF7\uFE0F ${state.couponCode}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { viewModel.removeCoupon() }) {
                                Text(
                                    text = "\u2715 Remove",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    } else if (showCouponField) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OneTillTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it },
                                placeholder = "Enter coupon code",
                                modifier = Modifier.weight(1f),
                            )
                            OneTillButton(
                                text = "Apply",
                                onClick = {
                                    viewModel.applyCoupon(couponInput)
                                    showCouponField = false
                                },
                                variant = ButtonVariant.Secondary,
                                modifier = Modifier.weight(0.4f),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCouponField = true }
                                .padding(vertical = dimens.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "\uD83C\uDFF7\uFE0F Add Coupon Code",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(text = "+", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Totals section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = dimens.lg, vertical = dimens.md),
        ) {
            TotalRow("Subtotal", CartViewModel.formatCents(state.subtotalCents))
            if (state.discountCents > 0) {
                TotalRow(
                    label = "Discount (${state.couponCode})",
                    value = "-${CartViewModel.formatCents(state.discountCents)}",
                    isDiscount = true,
                )
            }
            TotalRow("Tax (10%)", CartViewModel.formatCents(state.taxCents))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = dimens.sm),
                color = MaterialTheme.colorScheme.outline,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Total", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = CartViewModel.formatCents(state.totalCents),
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }

        // Bottom action bar
        BottomActionBar {
            OneTillButton(
                text = "Charge ${CartViewModel.formatCents(state.totalCents)}",
                onClick = onCheckout,
                enabled = state.items.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    isDiscount: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            color = if (isDiscount) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}
