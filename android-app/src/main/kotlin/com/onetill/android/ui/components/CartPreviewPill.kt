package com.onetill.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun CartPreviewPill(
    itemCount: Int,
    totalFormatted: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val pillShape = RoundedCornerShape(26.dp)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(12.dp, pillShape)
                .clip(pillShape)
                .background(colors.accent)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = "$itemCount items in cart, total $totalFormatted"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left section — cart icon + total + item count
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CartIcon(
                    color = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = totalFormatted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "$itemCount items",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }

            // Right section — frosted checkout button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Checkout",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun CartIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val s = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.06f, h * 0.12f)
            lineTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(w * 0.85f, h * 0.65f)
            lineTo(w * 0.94f, h * 0.25f)
            lineTo(w * 0.28f, h * 0.25f)
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
        drawCircle(color = color, radius = w * 0.06f, center = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.82f))
        drawCircle(color = color, radius = w * 0.06f, center = androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.82f))
    }
}
