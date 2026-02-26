package com.onetill.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.onetill.android.ui.theme.OneTillTheme

@Composable
fun CartLineItem(
    name: String,
    variationInfo: String?,
    imageUrl: String?,
    quantity: Int,
    lineTotalFormatted: String,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = OneTillTheme.dimens

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.lg, vertical = dimens.md),
            horizontalArrangement = Arrangement.spacedBy(dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Product image
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(dimens.productImageRadius)),
                contentScale = ContentScale.Crop,
            )

            // Name + variation + stepper
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.xs),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (variationInfo != null) {
                    Text(
                        text = variationInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = onQuantityChange,
                )
            }

            // Line total
            Text(
                text = lineTotalFormatted,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dimens.lg),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
