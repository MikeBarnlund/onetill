package com.onetill.android.ui.complete

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onetill.android.ui.components.BottomActionBar
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.theme.OneTillTheme
import kotlinx.coroutines.delay

@Composable
fun OrderCompleteScreen(
    amount: String,
    paymentMethod: String,
    onNewSale: () -> Unit,
) {
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors

    // Checkmark scale animation
    var animateCheck by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animateCheck) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "checkmark_scale",
    )

    LaunchedEffect(Unit) {
        animateCheck = true
    }

    // Auto-advance after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        onNewSale()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Green checkmark
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(colors.success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2713",
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.textPrimary,
                )
            }

            Spacer(modifier = Modifier.height(dimens.xl))

            Text(
                text = "Payment Complete",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(dimens.md))

            Text(
                text = amount,
                style = MaterialTheme.typography.displayLarge,
            )

            Spacer(modifier = Modifier.height(dimens.sm))

            Text(
                text = paymentMethod,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        BottomActionBar {
            OneTillButton(
                text = "New Sale",
                onClick = onNewSale,
            )
        }
    }
}
