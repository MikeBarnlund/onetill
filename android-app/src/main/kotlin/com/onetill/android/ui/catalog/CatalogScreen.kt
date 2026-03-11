package com.onetill.android.ui.catalog

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onetill.android.R
import com.onetill.android.audio.ScanBeepPlayer
import com.onetill.android.input.VolumeKeyEvent
import com.onetill.android.input.VolumeKeyEventBus
import com.onetill.android.ui.components.BarcodeIcon
import com.onetill.android.ui.components.ButtonVariant
import org.koin.androidx.compose.koinViewModel
import com.onetill.android.ui.components.CartPreviewPill
import com.onetill.android.ui.components.CloseIcon
import com.onetill.android.ui.components.AppStatusBar
import com.onetill.android.ui.components.HeaderActionButton
import com.onetill.android.ui.components.HeaderNavAction
import com.onetill.android.ui.components.NavigationDrawer
import com.onetill.android.ui.components.OneTillButton
import com.onetill.android.ui.components.ProductCard
import com.onetill.android.ui.components.SearchIcon
import com.onetill.android.ui.components.ToastHost
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.util.formatDisplay
import com.onetill.android.ui.theme.OneTillTheme
import com.onetill.android.ui.theme.screenGradientBackground
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    viewModel: CatalogViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    val products by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    val pickerProduct by viewModel.pickerProduct.collectAsState()
    val isPickerVisible by viewModel.isPickerVisible.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isScannerOpen by viewModel.isScannerOpen.collectAsState()
    val hasLoaded by viewModel.hasLoaded.collectAsState()
    val registerName by viewModel.registerName.collectAsState()

    val haptic = LocalHapticFeedback.current
    var showWifiPasscodeDialog by remember { mutableStateOf(false) }

    // Activate volume key interception while on CatalogScreen
    DisposableEffect(Unit) {
        VolumeKeyEventBus.isActive = true
        onDispose { VolumeKeyEventBus.isActive = false }
    }

    // Volume button → scanner toggle / hold-to-scan
    LaunchedEffect(Unit) {
        var pressTime = 0L
        var wasOpenBeforePress = false
        VolumeKeyEventBus.events.collect { event ->
            when (event) {
                is VolumeKeyEvent.Pressed -> {
                    pressTime = System.currentTimeMillis()
                    wasOpenBeforePress = isScannerOpen
                    if (!wasOpenBeforePress) viewModel.openScanner()
                }
                is VolumeKeyEvent.Released -> {
                    val held = System.currentTimeMillis() - pressTime
                    if (held > 300) {
                        // Long press (hold-to-scan) — close on release
                        viewModel.closeScanner()
                    } else if (wasOpenBeforePress) {
                        // Short press while already open — toggle off
                        viewModel.closeScanner()
                    }
                    // Short press while closed — scanner was opened on press, stays open
                }
            }
        }
    }

    val drawerWidthPx = with(LocalDensity.current) { dimens.drawerWidth.roundToPx() }

    val offsetAnim by animateIntAsState(
        targetValue = if (isDrawerOpen) drawerWidthPx else 0,
        animationSpec = tween(
            durationMillis = if (isDrawerOpen) 250 else 200,
            easing = FastOutSlowInEasing,
        ),
        label = "drawerOffset",
    )

    if (!hasLoaded) {
        SplashScreen()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Drawer (always rendered behind main content)
        NavigationDrawer(
            versionText = "v1.0 · $registerName",
            onOrdersTap = {
                viewModel.closeDrawer()
                onNavigateToOrders()
            },
            onSummaryTap = {
                viewModel.closeDrawer()
                onNavigateToSummary()
            },
            onScanQrTap = {
                viewModel.closeDrawer()
                onNavigateToQrScan()
            },
            onEditShopTap = {
                viewModel.closeDrawer()
                onNavigateToSettings()
            },
            onWifiSettingsTap = { showWifiPasscodeDialog = true },
            onResyncTap = {
                viewModel.closeDrawer()
                viewModel.fullResync()
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
                AppStatusBar()

                CatalogHeader(
                    isSearchVisible = isSearchVisible,
                    searchQuery = searchQuery,
                    onMenuTap = { viewModel.toggleDrawer() },
                    onSearchTap = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.onSearch(it) },
                    onSearchDismiss = { viewModel.dismissSearch() },
                    onBarcodeTap = { viewModel.toggleScanner() },
                )

                // Product grid
                PullToRefreshBox(
                    isRefreshing = isSyncing,
                    onRefresh = { viewModel.syncProducts() },
                    modifier = Modifier.weight(1f),
                ) {
                    if (products.isEmpty()) {
                        EmptyCatalogState(
                            isSyncing = isSyncing,
                            onSyncProducts = { viewModel.syncProducts() },
                            onNavigateToSettings = onNavigateToSettings,
                        )
                    } else {
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
                                val stock = if (product.type == ProductType.VARIABLE) {
                                    product.variants.sumOf { it.stockQuantity ?: 0 }
                                } else {
                                    product.stockQuantity ?: 0
                                }
                                val hasStockManagement = if (product.type == ProductType.VARIABLE) {
                                    product.variants.any { it.manageStock }
                                } else {
                                    product.manageStock
                                }
                                val isOutOfStock = hasStockManagement && stock <= 0
                                val priceFormatted = if (product.type == ProductType.VARIABLE) {
                                    "From ${product.price.formatDisplay()}"
                                } else {
                                    product.price.formatDisplay()
                                }
                                ProductCard(
                                    name = product.name,
                                    priceFormatted = priceFormatted,
                                    stockText = "$stock left",
                                    imageUrl = product.images.firstOrNull()?.url,
                                    isOutOfStock = isOutOfStock,
                                    onClick = { viewModel.onProductTap(product) },
                                )
                            }
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

        // Variation picker sheet overlay
        val currentPickerProduct = pickerProduct
        if (currentPickerProduct != null) {
            VariationPickerSheet(
                product = currentPickerProduct.toPickerProduct(),
                visible = isPickerVisible,
                onDismiss = { viewModel.dismissPicker() },
                onAddToCart = { selections -> viewModel.onVariantAddToCart(selections) },
            )
        }

        // Barcode scanner overlay
        BarcodeScannerOverlay(
            visible = isScannerOpen,
            onBarcodeScanned = { barcode ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ScanBeepPlayer.play()
                viewModel.onBarcodeScan(barcode)
            },
            onClose = { viewModel.closeScanner() },
        )

        // Toast — rendered last so it appears above scanner overlay
        ToastHost(
            state = viewModel.toastState,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // WiFi passcode dialog
        if (showWifiPasscodeDialog) {
            AlertDialog(
                onDismissRequest = { showWifiPasscodeDialog = false },
                title = { Text("Device Settings Passcode") },
                text = {
                    Text("The admin passcode for this terminal is:\n\n07139\n\nEnter this passcode on the next screen to access WiFi and other device settings.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showWifiPasscodeDialog = false
                        viewModel.closeDrawer()
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("stripe://settings/"))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWifiPasscodeDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyCatalogState(
    isSyncing: Boolean,
    onSyncProducts: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val colors = OneTillTheme.colors
    val dimens = OneTillTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No products yet",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(dimens.sm))

        Text(
            text = "Sync your WooCommerce catalog or check your store settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimens.xl))

        OneTillButton(
            text = if (isSyncing) "Syncing..." else "Sync Products",
            onClick = onSyncProducts,
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(dimens.sm))

        OneTillButton(
            text = "Store Settings",
            onClick = onNavigateToSettings,
            variant = ButtonVariant.Ghost,
        )
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

@Composable
private fun SplashScreen() {
    val colors = OneTillTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenGradientBackground(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.onetill_logo),
            contentDescription = "OneTill",
            modifier = Modifier.size(72.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading product catalog...",
            fontSize = 14.sp,
            color = colors.textSecondary,
        )
    }
}
