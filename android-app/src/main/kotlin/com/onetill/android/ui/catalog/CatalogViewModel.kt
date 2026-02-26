package com.onetill.android.ui.catalog

import androidx.lifecycle.ViewModel
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductStatus
import com.onetill.shared.data.model.ProductType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant

class CatalogViewModel : ViewModel() {

    private val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())

    private val fakeProducts = listOf(
        fakeProduct(1, "Organic Honey 500g", 1499, 24),
        fakeProduct(2, "Sourdough Bread Loaf", 899, 12),
        fakeProduct(3, "Free Range Eggs (Dozen)", 699, 36),
        fakeProduct(4, "Handmade Candle - Lavender", 1999, 8),
        fakeProduct(5, "Fresh Basil Bunch", 349, 0),
        fakeProduct(6, "Artisan Cheese Wheel", 2499, 5),
        fakeProduct(7, "Mixed Berry Jam 250ml", 799, 18),
        fakeProduct(8, "Beeswax Food Wraps (3pk)", 1599, 15),
        fakeProduct(9, "Roasted Coffee Beans 250g", 1899, 22),
        fakeProduct(10, "Ceramic Mug - Blue", 2199, 3),
    )

    private val _products = MutableStateFlow(fakeProducts)
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(fakeProducts)
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()

    private val _cartTotal = MutableStateFlow("$0.00")
    val cartTotal: StateFlow<String> = _cartTotal.asStateFlow()

    fun onSearch(query: String) {
        _searchQuery.value = query
        _searchResults.value = if (query.isBlank()) {
            fakeProducts
        } else {
            fakeProducts.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun onProductTap(product: Product) {
        _cartItemCount.value += 1
        _cartTotal.value = formatCents((_cartItemCount.value * 1499).toLong())
    }

    fun onBarcodeScan(barcode: String) {
        val product = fakeProducts.find { it.barcode == barcode }
        if (product != null) onProductTap(product)
    }

    private fun fakeProduct(
        id: Long,
        name: String,
        priceCents: Long,
        stock: Int,
    ) = Product(
        id = id,
        name = name,
        sku = "SKU-$id",
        barcode = "BAR-$id",
        price = Money(priceCents, "AUD"),
        regularPrice = Money(priceCents, "AUD"),
        salePrice = null,
        stockQuantity = stock,
        manageStock = true,
        status = ProductStatus.PUBLISHED,
        images = emptyList(),
        categories = emptyList(),
        variants = emptyList(),
        type = ProductType.SIMPLE,
        createdAt = now,
        updatedAt = now,
    )

    companion object {
        fun formatCents(cents: Long): String {
            val dollars = cents / 100
            val remainder = cents % 100
            return "$${dollars}.${remainder.toString().padStart(2, '0')}"
        }
    }
}
