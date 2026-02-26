package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val dimens = OneTillTheme.dimens
    val colors = OneTillTheme.colors
    val cardShape = RoundedCornerShape(dimens.cardRadius)
    val imageShape = RoundedCornerShape(
        topStart = dimens.productImageRadius,
        topEnd = dimens.productImageRadius,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        // Image area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(if (isOutOfStock) Modifier.alpha(0.5f) else Modifier),
                    contentScale = ContentScale.Crop,
                )
            } else if (isOutOfStock) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .alpha(0.5f),
                )
            }

            if (isOutOfStock) {
                StatusChip(
                    text = "Out of Stock",
                    variant = ChipVariant.Error,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(dimens.sm),
                )
            }
        }

        // Text area
        Column(
            modifier = Modifier.padding(dimens.md),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = priceFormatted,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stockText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOutOfStock) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
