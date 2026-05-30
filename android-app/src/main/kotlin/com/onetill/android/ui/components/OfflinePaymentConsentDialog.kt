package com.onetill.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Liability/consent dialog for enabling offline card payments. Shared by the
// settings screen, the setup wizard's offline-payments step, and the
// just-in-time enable prompt at checkout so the risk copy lives in one place.
@Composable
fun OfflinePaymentConsentDialog(
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
            text = "•  ",
            fontSize = 14.sp,
        )
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
