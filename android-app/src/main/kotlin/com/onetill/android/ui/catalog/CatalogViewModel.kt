package com.onetill.android.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.ui.components.ToastState
import com.onetill.android.ui.components.ToastType
import com.onetill.shared.cart.AddResult
import com.onetill.shared.cart.CartManager
import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.sync.SyncOrchestrator
import com.onetill.shared.util.formatDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class ProductUiModel(
    val id: Long,
    val name: String,
    val priceFormatted: String,
    val stockText: String,
    val imageUrl: String?,
    val isOutOfStock: Boolean,
    val product: Product,
)

class CatalogViewModel(
    private val localDataSource: LocalDataSource,
    private val cartManager: CartManager,
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    private val allProducts: StateFlow<List<Product>> =
        localDataSource.observeAllProducts()
            .onEach { _hasLoaded.value = true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<ProductUiModel>> =
        combine(allProducts, _searchQuery.debounce(150)) { products, query ->
            val filtered = if (query.isBlank()) products
            else {
                val nameMatches = mutableListOf<Product>()
                val categoryMatches = mutableListOf<Product>()
                val tagMatches = mutableListOf<Product>()
                for (product in products) {
                    when {
                        product.name.contains(query, ignoreCase = true) ->
                            nameMatches.add(product)
                        product.categories.any { it.name.contains(query, ignoreCase = true) } ->
                            categoryMatches.add(product)
                        product.tags.any { it.name.contains(query, ignoreCase = true) } ->
                            tagMatches.add(product)
                    }
                }
                nameMatches + categoryMatches + tagMatches
            }
            filtered.map { product -> product.toUiModel() }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItemCount: StateFlow<Int> =
        cartManager.cartState
            .map { it.itemCount }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotal: StateFlow<String> =
        cartManager.cartState
            .map { it.estimatedTotal.formatDisplay() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$0.00")

    val toastState = ToastState()

    val registerName: StateFlow<String> =
        localDataSource.observeStoreConfig()
            .map { it?.registerName ?: "Register 1" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Register 1")

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _pickerProduct = MutableStateFlow<Product?>(null)
    val pickerProduct: StateFlow<Product?> = _pickerProduct.asStateFlow()

    private val _isPickerVisible = MutableStateFlow(false)
    val isPickerVisible: StateFlow<Boolean> = _isPickerVisible.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isScannerOpen = MutableStateFlow(false)
    val isScannerOpen: StateFlow<Boolean> = _isScannerOpen.asStateFlow()

    private val _isCustomSaleVisible = MutableStateFlow(false)
    val isCustomSaleVisible: StateFlow<Boolean> = _isCustomSaleVisible.asStateFlow()

    fun syncProducts() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = withTimeout(10_000) {
                    syncOrchestrator.performDeltaSync()
                }
                if (result is AppResult.Error) {
                    toastState.show(result.message, ToastType.Error)
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                toastState.show("Sync timed out — check your connection", ToastType.Error)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun fullResync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = withTimeout(60_000) {
                    syncOrchestrator.performFullResync()
                }
                when (result) {
                    is AppResult.Success -> toastState.show("Full resync complete", ToastType.Success)
                    is AppResult.Error -> toastState.show(result.message, ToastType.Error)
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                toastState.show("Resync timed out — check your connection", ToastType.Error)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun toggleDrawer() {
        _isDrawerOpen.value = !_isDrawerOpen.value
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        }
    }

    fun dismissSearch() {
        _isSearchVisible.value = false
        _searchQuery.value = ""
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
    }

    fun onProductTap(product: Product) {
        when (product.type) {
            ProductType.SIMPLE -> {
                val result = cartManager.addProduct(product)
                if (result == AddResult.StockLimitReached) {
                    val stock = product.stockQuantity ?: 0
                    toastState.show("Only $stock in stock", ToastType.Warning)
                }
            }
            ProductType.VARIABLE -> {
                _pickerProduct.value = product
                _isPickerVisible.value = true
            }
        }
    }

    fun dismissPicker() {
        _isPickerVisible.value = false
    }

    fun onVariantAddToCart(selections: Map<String, String>) {
        val product = _pickerProduct.value ?: return
        val matchingVariant = product.variants.find { variant ->
            selections.all { (attrName, attrValue) ->
                variant.attributes.any { it.name == attrName && it.value == attrValue }
            }
        }
        val result = if (matchingVariant != null) {
            cartManager.addProduct(product, matchingVariant)
        } else {
            cartManager.addProduct(product)
        }
        if (result == AddResult.StockLimitReached) {
            val stock = matchingVariant?.stockQuantity ?: product.stockQuantity ?: 0
            toastState.show("Only $stock in stock", ToastType.Warning)
        }
        _isPickerVisible.value = false
    }

    fun openScanner() {
        _isScannerOpen.value = true
    }

    fun closeScanner() {
        _isScannerOpen.value = false
    }

    fun toggleScanner() {
        _isScannerOpen.value = !_isScannerOpen.value
    }

    fun onBarcodeScan(barcode: String) {
        viewModelScope.launch {
            val product = localDataSource.getProductByBarcode(barcode)
            if (product == null) {
                toastState.show("No product found for $barcode", ToastType.Warning)
                return@launch
            }

            // Check if the barcode matches a specific variant — add it directly
            val matchedVariant = product.variants.find { it.barcode == barcode }
            if (matchedVariant != null) {
                val result = cartManager.addProduct(product, matchedVariant)
                if (result == AddResult.StockLimitReached) {
                    val stock = matchedVariant.stockQuantity ?: 0
                    toastState.show("Only $stock in stock", ToastType.Warning)
                } else {
                    toastState.show("Added ${product.name} — ${matchedVariant.name}", ToastType.Success)
                }
                return@launch
            }

            // Barcode is on the parent product — show picker for variable, add directly for simple
            if (product.type == ProductType.SIMPLE) {
                val result = cartManager.addProduct(product)
                if (result == AddResult.StockLimitReached) {
                    val stock = product.stockQuantity ?: 0
                    toastState.show("Only $stock in stock", ToastType.Warning)
                } else {
                    toastState.show("Added ${product.name}", ToastType.Success)
                }
            } else {
                onProductTap(product)
            }
        }
    }

    fun openCustomSaleSheet() {
        _isCustomSaleVisible.value = true
    }

    fun dismissCustomSaleSheet() {
        _isCustomSaleVisible.value = false
    }

    fun addCustomSale(description: String, amountCents: Long) {
        val currency = cartManager.cartState.value.currency
        val amount = Money(amountCents = amountCents, currencyCode = currency)
        cartManager.addCustomSale(description, amount)
        _isCustomSaleVisible.value = false
        toastState.show("Custom sale added", ToastType.Success)
    }
}

private fun Product.toUiModel(): ProductUiModel {
    val stock = if (type == ProductType.VARIABLE) {
        variants.sumOf { it.stockQuantity ?: 0 }
    } else {
        stockQuantity ?: 0
    }
    val hasStockManagement = if (type == ProductType.VARIABLE) {
        variants.any { it.manageStock }
    } else {
        manageStock
    }
    return ProductUiModel(
        id = id,
        name = name,
        priceFormatted = if (type == ProductType.VARIABLE) {
            "From ${price.formatDisplay()}"
        } else {
            price.formatDisplay()
        },
        stockText = "$stock left",
        imageUrl = images.firstOrNull()?.url,
        isOutOfStock = hasStockManagement && stock <= 0,
        product = this,
    )
}
