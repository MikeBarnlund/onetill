package com.onetill.android.ui.cart

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.checkout.CheckoutViewModel
import com.onetill.android.ui.checkout.PaymentMethodUi
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.CardPaymentIcon
import com.onetill.android.ui.components.CartLineItem
import com.onetill.android.ui.components.CashPaymentIcon
import com.onetill.shared.cart.CustomSaleItem
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.PaymentMethodCard
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground
import com.onetill.shared.data.model.CouponType
import com.onetill.shared.util.formatDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCashPayment: () -> Unit,
    onCardPaymentComplete: (orderId: Long, amount: String) -> Unit,
    onCardPaymentFailed: (message: String) -> Unit = {},
    viewModel: CartViewModel = koinViewModel(),
    checkoutViewModel: CheckoutViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val state by viewModel.cartState.collectAsState()
    val selectedMethod by checkoutViewModel.selectedPaymentMethod.collectAsState()
    val isOnline by checkoutViewModel.isOnline.collectAsState()
    val offlinePaymentsEnabled by checkoutViewModel.offlinePaymentsEnabled.collectAsState()
    val isSubmitting by checkoutViewModel.isSubmitting.collectAsState()
    val cardPaymentError by checkoutViewModel.cardPaymentError.collectAsState()
    val couponError by viewModel.couponError.collectAsState()
    var showCouponField by remember { mutableStateOf(false) }
    var couponInput by remember { mutableStateOf("") }

    // Auto-navigate back when cart becomes empty (after remove or clear).
    // Guard: if a payment method was selected, the cart was cleared by a sale —
    // navigation is handled by the payment callback, not by auto-back.
    val itemCount = state.itemCount
    var hadItems by remember { mutableStateOf(itemCount > 0) }
    LaunchedEffect(itemCount) {
        if (hadItems && itemCount == 0 && selectedMethod == null) onBack()
        if (itemCount > 0) hadItems = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        AppStatusBar()

        // Header — Back + "Cart (N items)" + "Clear All"
        ScreenHeader(
            title = "Cart (${state.itemCount} items)",
            navAction = HeaderNavAction.Back,
            onNavAction = onBack,
            rightActions = {
                OneTillButton(
                    text = "Clear All",
                    onClick = { viewModel.clearCart() },
                    variant = ButtonVariant.Destructive,
                )
            },
        )

        // Scrollable line items + coupon
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimens.md),
        ) {
            itemsIndexed(
                items = state.items,
                key = { _, item -> "${item.productId}_${item.variantId ?: 0}" },
            ) { index, item ->
                CartLineItem(
                    name = item.name,
                    variationInfo = item.variantId?.let { "Variant" },
                    imageUrl = item.imageUrl,
                    quantity = item.quantity,
                    lineTotalFormatted = item.totalPrice.formatDisplay(),
                    onQuantityChange = { viewModel.updateQuantity(item.productId, item.variantId, it) },
                    onRemove = { viewModel.removeItem(item.productId, item.variantId) },
                    maxQuantity = item.maxQuantity,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                if (index < state.items.lastIndex || state.customSaleItems.isNotEmpty()) {
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                }
            }

            // Custom sale items
            itemsIndexed(
                items = state.customSaleItems,
                key = { _, item -> "custom_${item.id}" },
            ) { index, item ->
                CustomSaleCartItem(
                    item = item,
                    onRemove = { viewModel.removeCustomSale(item.id) },
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                if (index < state.customSaleItems.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                }
            }

            // Coupon row
            item {
                HorizontalDivider(thickness = 1.dp, color = colors.border)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    val firstCoupon = state.appliedCoupons.firstOrNull()
                    if (firstCoupon != null) {
                        // Applied coupon — show code, description, discount amount, and remove
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CouponTagIcon(
                                    color = colors.success,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = firstCoupon.code,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                )
                                val label = when (firstCoupon.type) {
                                    CouponType.PERCENT -> "(${firstCoupon.amount}% off)"
                                    CouponType.FIXED_CART -> ""
                                    CouponType.FIXED_PRODUCT -> "(per item)"
                                }
                                if (label.isNotEmpty()) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = colors.textTertiary,
                                    )
                                }
                            }
                            Text(
                                text = "Remove",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.error,
                                modifier = Modifier.clickable { viewModel.removeCoupon(firstCoupon.code) },
                            )
                        }
                    } else if (showCouponField) {
                        // Inline coupon entry
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OneTillTextField(
                                    value = couponInput,
                                    onValueChange = {
                                        couponInput = it
                                        viewModel.clearCouponError()
                                    },
                                    placeholder = "Enter coupon code",
                                    modifier = Modifier.weight(1f),
                                )
                                OneTillButton(
                                    text = "Apply",
                                    onClick = {
                                        val applied = viewModel.applyCoupon(couponInput)
                                        if (applied) {
                                            couponInput = ""
                                            showCouponField = false
                                        }
                                    },
                                    variant = ButtonVariant.Secondary,
                                    modifier = Modifier.weight(0.4f),
                                )
                            }
                            if (couponError != null) {
                                Text(
                                    text = couponError!!,
                                    fontSize = 12.sp,
                                    color = colors.error,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                        }
                    } else {
                        // "Add Coupon Code" tappable row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCouponField = true }
                                .padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CouponTagIcon(
                                    color = colors.textSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "Add Coupon Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.textSecondary,
                                )
                            }
                            Text(
                                text = "+",
                                fontSize = 16.sp,
                                color = colors.textTertiary,
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }
        }

        // Totals section + Charge button (pinned at bottom)
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f))
                .padding(dimens.md),
        ) {
            // Subtotal
            TotalRow(
                label = "Subtotal",
                value = state.subtotal.formatDisplay(),
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Discount (only shown when a coupon is applied)
            if (state.discountTotal.amountCents > 0) {
                TotalRow(
                    label = "Discount",
                    value = "-${state.discountTotal.formatDisplay()}",
                    isDiscount = true,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Tax
            TotalRow(
                label = "Tax",
                value = state.estimatedTax.formatDisplay(),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Divider
            HorizontalDivider(thickness = 1.dp, color = colors.border)
            Spacer(modifier = Modifier.height(10.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    text = state.estimatedTotal.formatDisplay(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Payment method buttons
            val cardSubtitle = when {
                isSubmitting && selectedMethod == PaymentMethodUi.Card -> "Processing payment..."
                cardPaymentError != null -> cardPaymentError!!
                isOnline -> "Tap, chip, or swipe"
                offlinePaymentsEnabled -> "Offline — Tap or chip only"
                else -> "Requires internet"
            }

            PaymentMethodCard(
                title = "Card Payment",
                subtitle = cardSubtitle,
                subtitleColor = if (cardPaymentError != null) colors.error else null,
                icon = {
                    CardPaymentIcon(
                        color = colors.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                isSelected = selectedMethod == PaymentMethodUi.Card,
                onClick = {
                    if (isSubmitting || state.isEmpty) return@PaymentMethodCard
                    checkoutViewModel.selectPaymentMethod(PaymentMethodUi.Card)
                    if (isOnline || offlinePaymentsEnabled) {
                        checkoutViewModel.submitCardPayment(
                            onComplete = { orderId, amount -> onCardPaymentComplete(orderId, amount) },
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
                    if (state.isEmpty) return@PaymentMethodCard
                    checkoutViewModel.selectPaymentMethod(PaymentMethodUi.Cash)
                    onCashPayment()
                },
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
    val colors = OneTillTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDiscount) colors.success else colors.textTertiary,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (isDiscount) colors.success else colors.textSecondary,
        )
    }
}

@Composable
private fun CustomSaleCartItem(
    item: CustomSaleItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Placeholder icon — 56×56dp matching CartLineItem image size
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = colors.textTertiary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description.ifBlank { "Custom Sale" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Text(
                text = "Custom amount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Remove",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.error,
                    modifier = Modifier.clickable(onClick = onRemove),
                )
                Text(
                    text = item.amount.formatDisplay(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun CouponTagIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.08f, h * 0.5f)
            lineTo(w * 0.32f, h * 0.15f)
            lineTo(w * 0.92f, h * 0.15f)
            lineTo(w * 0.92f, h * 0.85f)
            lineTo(w * 0.32f, h * 0.85f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
        drawCircle(
            color = color,
            radius = w * 0.07f,
            center = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.5f),
        )
    }
}
