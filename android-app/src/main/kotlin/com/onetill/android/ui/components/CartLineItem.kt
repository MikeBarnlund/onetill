package com.onetill.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onRemove: () -> Unit,
    maxQuantity: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(intrinsicSize = androidx.compose.foundation.layout.IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Product image — 56×56dp, 8dp radius
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Content: name, variant, stepper + price
        Column(modifier = Modifier.weight(1f)) {
            // Product name — 13sp, weight 500, 1 line, ellipsis
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )

            // Variant text — 11sp, textTertiary
            if (variationInfo != null) {
                Text(
                    text = variationInfo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textTertiary,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stepper + line total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = onQuantityChange,
                    onRemove = onRemove,
                    maxQuantity = maxQuantity,
                )
                Text(
                    text = lineTotalFormatted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
    }
}
