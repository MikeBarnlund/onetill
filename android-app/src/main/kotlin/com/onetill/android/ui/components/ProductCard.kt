package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun ProductCard(
    name: String,
    priceFormatted: String,
    stockText: String,
    imageUrl: String?,
    isOutOfStock: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val imageShape = RoundedCornerShape(dimens.cardRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock, onClick = onClick),
    ) {
        // Image area — 4:3 aspect ratio with gradient scrim + name overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(imageShape)
                .background(colors.surface),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isOutOfStock) Modifier.alpha(0.3f) else Modifier),
                    contentScale = ContentScale.Crop,
                )
            }

            // Gradient scrim at bottom of image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.82f),
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )

            // Product name overlaid at bottom-left inside scrim
            Text(
                text = name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
            )

            // Out of stock overlay badge
            if (isOutOfStock) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.65f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.error,
                            letterSpacing = 0.06.sp,
                        )
                    }
                }
            }
        }

        // Info area below image
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isOutOfStock) "" else stockText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )
            Text(
                text = priceFormatted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOutOfStock) colors.textTertiary else colors.textPrimary,
            )
        }
    }
}
