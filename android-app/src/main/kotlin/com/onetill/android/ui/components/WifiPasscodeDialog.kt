package com.onetill.android.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Stripe S700/S710 admin passcode required to enter device-level Settings.
// Surfaced to the merchant whenever the app needs to send them to Wi-Fi or
// network configuration.
private const val DEVICE_SETTINGS_PASSCODE = "07139"
private const val STRIPE_SETTINGS_URI = "stripe://settings/"

@Composable
fun WifiPasscodeDialog(
    onDismiss: () -> Unit,
    onBeforeLaunch: () -> Unit = {},
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device Settings Passcode") },
        text = {
            Text(
                "The admin passcode for this terminal is:\n\n" +
                    "$DEVICE_SETTINGS_PASSCODE\n\n" +
                    "Enter this passcode on the next screen to access Wi-Fi " +
                    "and other device settings."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onBeforeLaunch()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(STRIPE_SETTINGS_URI))
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
