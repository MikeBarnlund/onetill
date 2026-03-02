package com.onetill.android.ui.catalog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.onetill.android.ui.components.CartIcon
import com.onetill.android.ui.components.VariationChip
import com.onetill.android.ui.theme.OneTillTheme
import kotlinx.coroutines.runBlocking

// UI models for the picker — decoupled from shared domain

data class PickerAttributeGroup(
    val name: String,
    val options: List<PickerOption>,
)

data class PickerOption(
    val label: String,
    val available: Boolean,
    val priceAdjustment: String? = null,
)

data class PickerProduct(
    val name: String,
    val imageUrl: String?,
    val startingPriceFormatted: String,
    val attributes: List<PickerAttributeGroup>,
    val resolvedPriceFormatted: String,
    val stockCount: Int,
)

/**
 * Variation Picker — modal bottom sheet overlaying the Catalog.
 *
 * Positioned so its top edge sits just below the Catalog header (~82dp),
 * filling the rest of the screen. Includes a 60% black scrim behind.
 *
 * @param product Stub product data for the picker.
 * @param visible Whether the sheet is shown.
 * @param onDismiss Called when the scrim is tapped or the sheet is swiped down.
 * @param onAddToCart Called with the selected attribute map when CTA is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariationPickerSheet(
    product: PickerProduct,
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    // Sheet offset: top of screen = header area = 28dp status bar + 52dp header + 2dp ≈ 82dp
    val sheetTopDp = 82.dp
    val sheetTopPx = with(LocalDensity.current) { sheetTopDp.roundToPx() }
    val sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    // Track selected option per attribute group
    val selections = remember {
        mutableStateMapOf<String, String>().apply {
            product.attributes.forEach { group ->
                val default = group.options.firstOrNull { it.available }
                if (default != null) put(group.name, default.label)
            }
        }
    }

    // Animatable offset: 0f = fully shown, 1f = fully hidden
    val offsetAnimatable = remember { Animatable(1f) }

    LaunchedEffect(visible) {
        offsetAnimatable.animateTo(
            targetValue = if (visible) 0f else 1f,
            animationSpec = tween(
                durationMillis = if (visible) 250 else 200,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    val animatedOffset = offsetAnimatable.value

    // Inner scroll state — shared with verticalScroll and the nested scroll connection
    val scrollState = rememberScrollState()

    // NestedScrollConnection: intercept downward drags at scroll-top to move the sheet
    val density = LocalDensity.current
    val nestedScrollConnection = remember(density) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy > 0f && scrollState.value == 0) {
                    // Dragging down while content is at top — move the sheet
                    val sheetHeightPx = with(density) { (1920 - 82).dp.toPx() }
                    val delta = dy / sheetHeightPx
                    val newValue = (offsetAnimatable.value + delta).coerceIn(0f, 1f)
                    val consumed = newValue - offsetAnimatable.value
                    runBlocking {
                        offsetAnimatable.snapTo(newValue)
                    }
                    return Offset(0f, consumed * sheetHeightPx)
                }
                if (dy < 0f && offsetAnimatable.value > 0f) {
                    // Dragging up while sheet is partially dismissed — snap it back first
                    val sheetHeightPx = with(density) { (1920 - 82).dp.toPx() }
                    val delta = dy / sheetHeightPx
                    val newValue = (offsetAnimatable.value + delta).coerceIn(0f, 1f)
                    val consumed = newValue - offsetAnimatable.value
                    runBlocking {
                        offsetAnimatable.snapTo(newValue)
                    }
                    return Offset(0f, consumed * sheetHeightPx)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetAnimatable.value > 0f) {
                    val velocityThreshold = with(density) { 800.dp.toPx() }
                    if (offsetAnimatable.value > 0.25f || available.y > velocityThreshold) {
                        onDismiss()
                    } else {
                        offsetAnimatable.animateTo(
                            0f,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    if (animatedOffset >= 1f && !visible) return

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim — 60% black, tappable to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * (1f - animatedOffset)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // Bottom sheet card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = sheetTopDp)
                .offset {
                    IntOffset(
                        0,
                        (animatedOffset * (1920 - sheetTopPx)).toInt(),
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(sheetShape)
                    .nestedScroll(nestedScrollConnection)
                    .background(colors.background),
            ) {
                // Scrollable area: hero image + variant sections + summary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                ) {
                    // Product hero image — 16:10 aspect, edge-to-edge, top corners 16dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    ) {
                        // Image
                        if (product.imageUrl != null) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.surface),
                            )
                        }

                        // Drag handle — 44dp tall touch target, visual bar stays 36x4dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.5f),
                                        RoundedCornerShape(2.dp),
                                    ),
                            )
                        }

                        // Gradient scrim at bottom of image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.55f),
                                            Color.Black.copy(alpha = 0.88f),
                                        ),
                                    ),
                                ),
                        )

                        // Product name + starting price overlaid in scrim
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = product.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                lineHeight = 21.sp,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Starting at ${product.startingPriceFormatted}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.textSecondary,
                            )
                        }
                    }

                    // Variant attribute sections
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp,
                        ),
                    ) {
                        product.attributes.forEach { group ->
                            // Section label — uppercase
                            Text(
                                text = group.name.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textTertiary,
                                letterSpacing = 0.66.sp,
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Chips — flex-wrap row, 8dp gap
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                group.options.forEach { option ->
                                    VariationChip(
                                        label = option.label,
                                        isSelected = selections[group.name] == option.label,
                                        isAvailable = option.available,
                                        priceAdjustment = option.priceAdjustment,
                                        onClick = {
                                            selections[group.name] = option.label
                                        },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // Selected variant summary strip
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 10.dp,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: selected combination + stock
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val selectionText = selections.values.joinToString(" · ")
                            Text(
                                text = selectionText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${product.stockCount} in stock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.success,
                            )
                        }

                        // Right: resolved price
                        Text(
                            text = product.resolvedPriceFormatted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }
                }

                // "Add to Cart" CTA — pinned below scroll area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
                ) {
                    Button(
                        onClick = { onAddToCart(selections.toMap()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics { contentDescription = "Add to Cart" },
                        shape = RoundedCornerShape(26.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        CartIcon(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Cart",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
