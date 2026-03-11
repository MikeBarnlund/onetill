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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.CardPaymentIcon
import org.koin.androidx.compose.koinViewModel
import com.onetill.android.ui.components.CashPaymentIcon
import com.onetill.android.ui.components.CheckmarkIcon
import com.onetill.android.ui.components.ChevronDownIcon
import com.onetill.android.ui.components.ChevronUpIcon
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.MailIcon
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.PaymentMethodCard
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradient

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onCashPayment: () -> Unit,
    onCardPaymentComplete: (String) -> Unit,
    onCardPaymentFailed: (String) -> Unit = {},
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val orderTotal by viewModel.orderTotalFormatted.collectAsState()
    val items by viewModel.items.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val selectedMethod by viewModel.selectedPaymentMethod.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var orderSummaryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
    ) {
        AppStatusBar()

        ScreenHeader(
            title = "Checkout",
            navAction = HeaderNavAction.Back,
            onNavAction = onBack,
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.md),
        ) {
            // ── Customer Email (optional) ──
            SectionLabel(text = "Customer email (optional)")
            Spacer(modifier = Modifier.height(8.dp))
            OneTillTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                placeholder = "customer@email.com",
                leadingIcon = {
                    MailIcon(
                        color = colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Payment Method ──
            SectionLabel(text = "Payment Method")
            Spacer(modifier = Modifier.height(10.dp))

            PaymentMethodCard(
                title = "Card Payment",
                subtitle = when {
                    isSubmitting && selectedMethod == PaymentMethodUi.Card -> "Processing payment..."
                    isOnline -> "Tap, chip, or swipe"
                    else -> "Requires internet"
                },
                icon = {
                    CardPaymentIcon(
                        color = colors.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                isSelected = selectedMethod == PaymentMethodUi.Card,
                onClick = {
                    if (isSubmitting) return@PaymentMethodCard
                    viewModel.selectPaymentMethod(PaymentMethodUi.Card)
                    if (isOnline) {
                        viewModel.submitCardPayment(
                            onComplete = { amount -> onCardPaymentComplete(amount) },
                            onFailed = { message -> onCardPaymentFailed(message) },
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentMethodCard(
                title = "Cash Payment",
                subtitle = "Enter amount received",
                icon = {
                    CashPaymentIcon(
                        color = colors.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                isSelected = selectedMethod == PaymentMethodUi.Cash,
                onClick = {
                    viewModel.selectPaymentMethod(PaymentMethodUi.Cash)
                    onCashPayment()
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Customer (optional) ──
            SectionLabel(text = "Customer (optional)")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckmarkIcon(
                    color = colors.success,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Guest checkout",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Order Summary (collapsible) ──
            HorizontalDivider(thickness = 1.dp, color = colors.border)

            Column(modifier = Modifier.animateContentSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { orderSummaryExpanded = !orderSummaryExpanded }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Order Summary ($itemCount items)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = orderTotal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        if (orderSummaryExpanded) {
                            ChevronUpIcon(
                                color = colors.textSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            ChevronDownIcon(
                                color = colors.textSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                if (orderSummaryExpanded) {
                    Column(
                        modifier = Modifier.padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items.forEach { item ->
                            val label = if (item.quantity > 1) {
                                "${item.name} ×${item.quantity}"
                            } else {
                                item.name
                            }
                            OrderSummaryRow(name = label, price = item.totalFormatted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.xxl))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = OneTillTheme.colors.textSecondary,
        letterSpacing = 0.6.sp,
    )
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
        )
        Text(
            text = price,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
        )
    }
}
