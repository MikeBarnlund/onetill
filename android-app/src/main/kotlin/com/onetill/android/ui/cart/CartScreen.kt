package com.onetill.android.ui.cart

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.CartLineItem
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradient
import com.onetill.shared.util.formatDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val state by viewModel.cartState.collectAsState()
    var showCouponField by remember { mutableStateOf(false) }
    var couponInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
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
                if (index < state.items.lastIndex) {
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
                    val firstCoupon = state.couponCodes.firstOrNull()
                    if (firstCoupon != null) {
                        // Applied coupon
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
                                    color = colors.textSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = firstCoupon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                )
                            }
                            Text(
                                text = "Remove",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.error,
                                modifier = Modifier.clickable { viewModel.removeCoupon(firstCoupon) },
                            )
                        }
                    } else if (showCouponField) {
                        // Inline coupon entry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
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
                                    couponInput = ""
                                    showCouponField = false
                                },
                                variant = ButtonVariant.Secondary,
                                modifier = Modifier.weight(0.4f),
                            )
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

            // Charge button
            OneTillButton(
                text = "Charge ${state.estimatedTotal.formatDisplay()}",
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
    val colors = OneTillTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.textTertiary,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (isDiscount) colors.error else colors.textSecondary,
        )
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
