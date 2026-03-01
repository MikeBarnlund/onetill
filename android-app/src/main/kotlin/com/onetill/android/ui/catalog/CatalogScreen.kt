package com.onetill.android.ui.catalog

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.BarcodeIcon
import com.onetill.android.ui.components.CartPreviewPill
import com.onetill.android.ui.components.CloseIcon
import com.onetill.android.ui.components.ConnectivityState
import com.onetill.android.ui.components.HeaderActionButton
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.NavigationDrawer
import com.onetill.android.ui.components.ProductCard
import com.onetill.android.ui.components.SearchIcon
import com.onetill.android.ui.components.StatusBar
import com.onetill.android.ui.theme.Background
import com.onetill.android.ui.theme.BackgroundGradientStart
import com.onetill.android.ui.theme.OneTillTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val products by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    val drawerWidthPx = with(LocalDensity.current) { dimens.drawerWidth.roundToPx() }

    val offsetAnim by animateIntAsState(
        targetValue = if (isDrawerOpen) drawerWidthPx else 0,
        animationSpec = tween(
            durationMillis = if (isDrawerOpen) 250 else 200,
            easing = FastOutSlowInEasing,
        ),
        label = "drawerOffset",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Drawer (always rendered behind main content)
        NavigationDrawer(
            onOrdersTap = {
                viewModel.closeDrawer()
                onNavigateToOrders()
            },
            onSummaryTap = {
                viewModel.closeDrawer()
                onNavigateToSummary()
            },
            onSettingsTap = {
                viewModel.closeDrawer()
                onNavigateToSettings()
            },
        )

        // Layer 2: Main content (slides right when drawer opens)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetAnim, 0) }
                .screenGradientBackground(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                StatusBar(
                    connectivityState = ConnectivityState.Online,
                    syncStatusText = "Synced",
                    batteryPercent = 85,
                    currentTime = "3:42",
                )

                CatalogHeader(
                    isSearchVisible = isSearchVisible,
                    searchQuery = searchQuery,
                    onMenuTap = { viewModel.toggleDrawer() },
                    onSearchTap = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.onSearch(it) },
                    onSearchDismiss = { viewModel.dismissSearch() },
                    onBarcodeTap = { /* barcode scanner trigger */ },
                )

                // Product grid
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { isRefreshing = false },
                    modifier = Modifier.weight(1f),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = dimens.md,
                            end = dimens.md,
                            top = 6.dp,
                            bottom = 72.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(dimens.md),
                        verticalArrangement = Arrangement.spacedBy(dimens.md),
                    ) {
                        items(
                            items = products,
                            key = { it.id },
                        ) { product ->
                            val stock = product.stockQuantity ?: 0
                            val isOutOfStock = product.manageStock && stock <= 0
                            ProductCard(
                                name = product.name,
                                priceFormatted = CatalogViewModel.formatCents(product.price.amountCents),
                                stockText = "$stock left",
                                imageUrl = product.images.firstOrNull()?.url,
                                isOutOfStock = isOutOfStock,
                                onClick = { viewModel.onProductTap(product) },
                            )
                        }
                    }
                }
            }

            // Cart preview pill — floating at bottom, aligned with grid margins
            CartPreviewPill(
                itemCount = cartItemCount,
                totalFormatted = cartTotal,
                visible = cartItemCount > 0,
                onClick = onNavigateToCart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = dimens.md, end = dimens.md, bottom = 8.dp),
            )

            // Invisible tap target to close drawer
            if (isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.closeDrawer() },
                        ),
                )
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    isSearchVisible: Boolean,
    searchQuery: String,
    onMenuTap: () -> Unit,
    onSearchTap: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchDismiss: () -> Unit,
    onBarcodeTap: () -> Unit,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.screenHeaderHeight)
            .padding(horizontal = dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Hamburger
        HeaderActionButton(
            navAction = HeaderNavAction.Menu,
            onClick = onMenuTap,
        )

        // Center: Expanding search — right edge stays anchored, left expands
        ExpandingSearchBox(
            expanded = isSearchVisible,
            query = searchQuery,
            onTap = onSearchTap,
            onQueryChange = onSearchQueryChange,
            onDismiss = onSearchDismiss,
            modifier = Modifier
                .weight(1f)
                .padding(start = dimens.sm),
        )

        Spacer(modifier = Modifier.width(dimens.sm))

        // Right: Barcode — always visible
        HeaderActionCircle(
            onClick = onBarcodeTap,
            contentDescription = "Scan barcode",
        ) {
            BarcodeIcon(
                color = colors.textSecondary,
                modifier = Modifier.size(dimens.headerIconSize),
            )
        }
    }
}

@Composable
private fun ExpandingSearchBox(
    expanded: Boolean,
    query: String,
    onTap: () -> Unit,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val pillShape = RoundedCornerShape(dimens.headerActionSize / 2)
    var showFieldContent by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            showFieldContent = false
            delay(210) // wait for width animation (200ms) to finish
            showFieldContent = true
        } else {
            showFieldContent = false
        }
    }

    LaunchedEffect(showFieldContent) {
        if (showFieldContent) focusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd,
    ) {
        val targetWidth = if (expanded) maxWidth else dimens.headerActionSize
        val animatedWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "searchExpand",
        )

        Box(
            modifier = Modifier
                .width(animatedWidth)
                .height(dimens.headerActionSize)
                .clip(pillShape)
                .background(colors.surface)
                .then(
                    if (!expanded) {
                        Modifier
                            .clickable(onClick = onTap)
                            .semantics { contentDescription = "Search products" }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = showFieldContent,
                animationSpec = tween(120),
                label = "searchContent",
            ) { ready ->
                if (ready) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchIcon(
                            color = colors.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary,
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() },
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Search products...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textTertiary,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = onDismiss)
                                .semantics { contentDescription = "Close search" },
                            contentAlignment = Alignment.Center,
                        ) {
                            CloseIcon(
                                color = colors.textTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        SearchIcon(
                            color = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderActionCircle(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Box(
        modifier = Modifier
            .size(dimens.headerActionSize)
            .background(colors.surface, RoundedCornerShape(dimens.headerActionSize / 2))
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun Modifier.screenGradientBackground(): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(BackgroundGradientStart, Background),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
    )
}
