package com.onetill.android.ui.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.MailIcon
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReceiptEmailScreen(
    orderId: Long,
    onSend: (email: String) -> Unit,
    onSkip: () -> Unit,
    viewModel: ReceiptEmailViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    var emailInput by remember { mutableStateOf("") }
    val isValidEmail = emailInput.contains("@") && emailInput.contains(".")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        AppStatusBar()

        ScreenHeader(title = "Digital Receipt")

        // Centered content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MailIcon(
                color = colors.textTertiary,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Send receipt?",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Email an order confirmation to the customer",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )

            Spacer(modifier = Modifier.height(24.dp))

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
        }

        // Buttons pinned at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.md, end = dimens.md, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "Send Receipt",
                onClick = {
                    viewModel.saveEmail(orderId, emailInput.trim())
                    onSend(emailInput.trim())
                },
                enabled = isValidEmail,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OneTillButton(
                text = "Skip",
                onClick = onSkip,
                variant = ButtonVariant.Ghost,
            )
        }
    }
}
