package com.onetill.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.ButtonVariant
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.ScreenHeader
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground
import org.koin.androidx.compose.koinViewModel

@Composable
fun OfflinePaymentSettingsScreen(
    onBack: () -> Unit,
    viewModel: OfflinePaymentSettingsViewModel = koinViewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        AppStatusBar()

        ScreenHeader(
            title = "Offline Payments",
            navAction = HeaderNavAction.Back,
            onNavAction = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.lg, vertical = dimens.lg),
            verticalArrangement = Arrangement.spacedBy(dimens.md),
        ) {
            // Warning banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.warning.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = "Offline payments carry risk. Payments accepted without internet may be declined when forwarded — you assume full liability.",
                    fontSize = 13.sp,
                    color = colors.warning,
                    lineHeight = 18.sp,
                )
            }

            // Toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable Offline Payments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                )
                Switch(
                    checked = state.enabled,
                    onCheckedChange = { viewModel.onToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                    ),
                )
            }

            if (state.enabled) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Limits",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )

                OneTillTextField(
                    value = state.perTransactionLimitFormatted,
                    onValueChange = { viewModel.onPerTransactionLimitChange(it) },
                    label = "Maximum per transaction ($)",
                    placeholder = "e.g. 500",
                )

                OneTillTextField(
                    value = state.totalLimitFormatted,
                    onValueChange = { viewModel.onTotalLimitChange(it) },
                    label = "Maximum total offline ($)",
                    placeholder = "e.g. 2000",
                )

                OneTillButton(
                    text = "Save Limits",
                    onClick = { viewModel.saveLimits(onSaved = onBack) },
                    enabled = state.perTransactionLimitFormatted.isNotBlank() &&
                        state.totalLimitFormatted.isNotBlank(),
                )

                Spacer(modifier = Modifier.height(dimens.md))

                // Status section
                Text(
                    text = "Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )

                Text(
                    text = "Pending offline payments: ${state.pendingOfflineCount}",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                )

                Text(
                    text = "Pending offline amount: ${state.pendingOfflineAmountFormatted}",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                )

                if (state.forwardingFailures > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                colors.error.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "${state.forwardingFailures} payment(s) were declined after being accepted offline",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.error,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }

    // Consent dialog
    if (state.showConsentDialog) {
        ConsentDialog(
            onAccept = { viewModel.onConsentAccepted() },
            onDecline = { viewModel.onConsentDeclined() },
        )
    }
}

@Composable
private fun ConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = {
            Text(
                text = "Enable Offline Payments",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "When enabled, this terminal will accept card payments without internet. " +
                        "Payments are stored on the device and forwarded to the bank when connectivity returns.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Text(
                    text = "You assume full liability for all offline payments. This means:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BulletPoint("Payments may be declined when forwarded — there is no recovery")
                    BulletPoint("You will have already provided goods/services for these payments")
                    BulletPoint("Stolen or fraudulent cards cannot be detected while offline")
                }
                Text(
                    text = "By enabling this feature, you acknowledge and accept these risks.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("I Accept the Risks & Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row {
        Text(
            text = "\u2022  ",
            fontSize = 14.sp,
        )
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
