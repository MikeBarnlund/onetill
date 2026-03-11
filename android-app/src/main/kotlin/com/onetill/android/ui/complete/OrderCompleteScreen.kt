package com.onetill.android.ui.complete

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.Success
import com.onetill.android.ui.theme.screenGradientBackground
import kotlinx.coroutines.delay

@Composable
fun OrderCompleteScreen(
    amount: String,
    paymentMethod: String,
    onNewSale: () -> Unit,
    changeDue: String? = null,
    receiptEmail: String? = null,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    // Checkmark scale animation — 0→1 with slight overshoot over 400ms
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = 400
                0f at 0
                1.1f at 280
                1f at 400
            },
        )
    }

    // Auto-advance after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        onNewSale()
    }

    // No status bar on this screen — it's a momentary celebration state
    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
    ) {
        // Centered content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Green circle with checkmark + subtle glow
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .drawBehind {
                        // Subtle green glow — layered translucent circles
                        val glowColor = Success.copy(alpha = 0.06f)
                        drawCircle(color = glowColor, radius = size.width / 2f + 30.dp.toPx())
                        drawCircle(color = glowColor, radius = size.width / 2f + 20.dp.toPx())
                        drawCircle(color = glowColor, radius = size.width / 2f + 10.dp.toPx())
                    }
                    .background(colors.success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SuccessCheckmark(
                    color = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "Payment Complete" — 20sp, weight 600
            Text(
                text = "Payment Complete",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Amount — 32sp, weight 700
            Text(
                text = amount,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Payment method — 14sp, textTertiary
            Text(
                text = paymentMethod,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )

            // Change due (cash payments only)
            if (changeDue != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Change due",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textTertiary,
                )
                Text(
                    text = changeDue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.success,
                )
            }

            // Receipt email (optional)
            if (receiptEmail != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Receipt sent to",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textTertiary,
                )
                Text(
                    text = receiptEmail,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                )
            }
        }

        // "New Sale" CTA — pinned at bottom, no BottomActionBar surface bg
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dimens.md, end = dimens.md, top = 10.dp, bottom = 14.dp),
        ) {
            OneTillButton(
                text = "New Sale",
                onClick = onNewSale,
            )
        }
    }
}

@Composable
private fun SuccessCheckmark(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 3.5.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.222f, h * 0.5f)
            lineTo(w * 0.417f, h * 0.722f)
            lineTo(w * 0.778f, h * 0.306f)
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
    }
}
