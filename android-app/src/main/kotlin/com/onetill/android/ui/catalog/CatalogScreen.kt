package com.onetill.android.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetill.android.ui.components.CartPreviewPill
import com.onetill.android.ui.components.ConnectivityState
import com.onetill.android.ui.components.OneTillTextField
import com.onetill.android.ui.components.ProductCard
import com.onetill.android.ui.components.StatusBar
import com.onetill.android.ui.theme.OneTillTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToDailySummary: () -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val dimens = OneTillTheme.dimens
    val products by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
            StatusBar(
                connectivityState = ConnectivityState.Online,
                syncStatusText = "Synced",
                batteryPercent = 85,
                currentTime = "3:42 PM",
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.screenHeaderHeight)
                    .padding(horizontal = dimens.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left spacer
                Box(modifier = Modifier.size(dimens.touchTargetSecondary))

                // Center title
                Text(
                    text = "OneTill",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )

                // Right icon buttons
                IconButton(
                    onClick = onNavigateToOrders,
                    modifier = Modifier.size(dimens.touchTargetSecondary),
                ) {
                    Text(
                        text = "\uD83E\uDDFE",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                IconButton(
                    onClick = onNavigateToDailySummary,
                    modifier = Modifier.size(dimens.touchTargetSecondary),
                ) {
                    Text(
                        text = "\u2699\uFE0F",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            // Search bar
            OneTillTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearch(it) },
                placeholder = "Search products or scan barcode",
                modifier = Modifier.padding(horizontal = dimens.lg, vertical = dimens.sm),
            )

            // Product grid with pull to refresh
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = false },
                modifier = Modifier.weight(1f),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = dimens.lg,
                        end = dimens.lg,
                        top = dimens.sm,
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
                            stockText = if (isOutOfStock) "Out of Stock" else "In Stock: $stock",
                            imageUrl = product.images.firstOrNull()?.url,
                            isOutOfStock = isOutOfStock,
                            onClick = { viewModel.onProductTap(product) },
                        )
                    }
                }
            }
        }

        // Cart preview pill
        CartPreviewPill(
            itemCount = cartItemCount,
            totalFormatted = cartTotal,
            visible = cartItemCount > 0,
            onClick = onNavigateToCart,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = dimens.lg),
        )
    }
}
