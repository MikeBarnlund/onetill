package com.onetill.android.ui.subscription

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
private fun WarningIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = 2.5.dp.toPx()

        // Triangle outline
        val triangle = Path().apply {
            moveTo(w / 2f, h * 0.08f)          // apex
            lineTo(w * 0.96f, h * 0.92f)        // bottom-right
            lineTo(w * 0.04f, h * 0.92f)        // bottom-left
            close()
        }
        drawPath(triangle, color = color, style = Stroke(width = s, cap = StrokeCap.Round))

        // Exclamation stem
        drawLine(
            color = color,
            start = Offset(w / 2f, h * 0.38f),
            end = Offset(w / 2f, h * 0.66f),
            strokeWidth = s,
            cap = StrokeCap.Round,
        )
        // Exclamation dot
        drawCircle(
            color = color,
            radius = s * 0.7f,
            center = Offset(w / 2f, h * 0.78f),
        )
    }
}

@Composable
fun SubscriptionExpiredScreen(
    onCheckAgain: suspend () -> Boolean,
    onNavigateToOrders: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToQrScan: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WarningIcon(
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Your OneTill subscription has expired",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Renew your subscription at onetill.app to continue selling.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    scope.launch {
                        isChecking = true
                        onCheckAgain()
                        isChecking = false
                    }
                },
                enabled = !isChecking,
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Check Again")
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            TextButton(onClick = onNavigateToOrders) {
                Text("View Order History")
            }
            TextButton(onClick = onNavigateToSettings) {
                Text("Settings")
            }
            TextButton(onClick = onNavigateToQrScan) {
                Text("Re-pair Device")
            }
        }
    }
}
