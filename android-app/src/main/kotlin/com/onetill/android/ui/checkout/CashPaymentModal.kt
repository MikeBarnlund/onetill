package com.onetill.android.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.shared.util.formatCents
import org.koin.androidx.compose.koinViewModel
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.NumberPad
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradient

@Composable
fun CashPaymentModal(
    onClose: () -> Unit,
    onPaymentComplete: (String) -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val orderTotalFormatted by viewModel.orderTotalFormatted.collectAsState()
    val orderTotalCents by viewModel.orderTotalCents.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    var amountText by remember { mutableStateOf("") }

    val amountCents = parseAmountCents(amountText)
    val changeCents = if (amountCents >= orderTotalCents) amountCents - orderTotalCents else 0L
    val canComplete = amountCents >= orderTotalCents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(brush = screenGradient(size.width, size.height)) },
    ) {
        // Status bar
        AppStatusBar()

        // Header — Close X + "Cash Payment"
        ScreenHeader(
            title = "Cash Payment",
            navAction = HeaderNavAction.Close,
            onNavAction = onClose,
        )

        // Total Due — centered
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.md, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "TOTAL DUE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textTertiary,
                letterSpacing = 0.6.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = orderTotalFormatted,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }

        // Amount Received — centered display box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.md, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "AMOUNT RECEIVED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textTertiary,
                letterSpacing = 0.6.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatAmountDisplay(amountText),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }

        // Change Due — appears when overpaid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.md, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (canComplete && changeCents > 0) {
                Text(
                    text = "Change due: ${formatCents(changeCents)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.success,
                )
            } else {
                // Reserve space so layout doesn't shift
                Spacer(modifier = Modifier.height(17.dp))
            }
        }

        // Number Pad — centered in remaining flex space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.xl),
            contentAlignment = Alignment.Center,
        ) {
            NumberPad(
                onDigit = { digit ->
                    amountText = appendDigit(amountText, digit)
                },
                onDot = {
                    if (!amountText.contains('.')) {
                        amountText = if (amountText.isEmpty()) "0." else "$amountText."
                    }
                },
                onBackspace = {
                    if (amountText.isNotEmpty()) {
                        amountText = amountText.dropLast(1)
                    }
                },
            )
        }

        // Complete Sale — primary CTA pinned at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.md, end = dimens.md, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "Complete Sale",
                onClick = {
                    viewModel.submitCashPayment { amount -> onPaymentComplete(amount) }
                },
                enabled = canComplete && !isSubmitting,
            )
        }
    }
}

private fun parseAmountCents(text: String): Long {
    if (text.isEmpty()) return 0
    val value = text.toDoubleOrNull() ?: return 0
    return (value * 100).toLong()
}

private fun formatAmountDisplay(text: String): String {
    if (text.isEmpty()) return "$0.00"
    val value = text.toDoubleOrNull() ?: return "$$text"
    // If user is still typing decimals, show what they've typed
    return if (text.contains('.')) {
        val parts = text.split('.')
        val decPart = parts.getOrElse(1) { "" }
        when (decPart.length) {
            0 -> "$$text"
            1 -> "$$text"
            else -> "$${String.format("%.2f", value)}"
        }
    } else {
        "$$text"
    }
}

private fun appendDigit(current: String, digit: Char): String {
    // Limit to 2 decimal places
    if (current.contains('.')) {
        val decPart = current.substringAfter('.')
        if (decPart.length >= 2) return current
    }
    // Prevent leading zeros (except "0.")
    if (current == "0" && digit != '.') return digit.toString()
    return current + digit
}
