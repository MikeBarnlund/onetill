package com.onetill.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.onetill.shared.data.model.ConsentAction
import com.onetill.shared.data.model.ConsentLogEntry
import com.onetill.shared.data.model.Coupon
import com.onetill.shared.data.model.CouponType
import com.onetill.shared.data.model.FeeLine
import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.OfflinePaymentConfig
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductCategory
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.data.model.ProductImage
import com.onetill.shared.data.model.ProductStatus
import com.onetill.shared.data.model.ProductTag
import com.onetill.shared.data.model.StaffUser
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.data.model.ProductVariant
import com.onetill.shared.data.model.TaxRate
import com.onetill.shared.data.model.VariantAttribute
import com.onetill.shared.db.OneTillDb
import com.onetill.shared.db.Product_cache
import com.onetill.shared.db.Offline_orders
import com.onetill.shared.db.Store_config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightLocalDataSource(private val db: OneTillDb) : LocalDataSource {

    private val queries get() = db.oneTillQueries

    // ========================================================================
    // Coupons
    // ========================================================================

    override suspend fun saveCoupons(coupons: List<Coupon>) = withContext(Dispatchers.Default) {
        db.transaction {
            queries.deleteAllCoupons()
            for (coupon in coupons) {
                queries.insertOrReplaceCoupon(
                    id = coupon.id,
                    code = coupon.code,
                    type = coupon.type.name,
                    amount = coupon.amount,
                )
            }
        }
    }

    override suspend fun getAllCoupons(): List<Coupon> = withContext(Dispatchers.Default) {
        queries.selectAllCoupons().executeAsList().map {
            Coupon(
                id = it.id,
                code = it.code,
                type = CouponType.valueOf(it.type),
                amount = it.amount,
            )
        }
    }

    override suspend fun getCouponByCode(code: String): Coupon? = withContext(Dispatchers.Default) {
        queries.selectCouponByCode(code).executeAsOneOrNull()?.let {
            Coupon(
                id = it.id,
                code = it.code,
                type = CouponType.valueOf(it.type),
                amount = it.amount,
            )
        }
    }

    // ========================================================================
    // Products
    // ========================================================================

    override fun observeAllProducts(): Flow<List<Product>> =
        queries.selectAllProducts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { assembleProduct(it) } }

    override suspend fun getAllProducts(): List<Product> = withContext(Dispatchers.Default) {
        queries.selectAllProducts().executeAsList().map { assembleProduct(it) }
    }

    override suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.Default) {
        queries.selectProductById(id).executeAsOneOrNull()?.let { assembleProduct(it) }
    }

    override suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.Default) {
        queries.selectProductByBarcode(barcode, barcode).executeAsOneOrNull()?.let { assembleProduct(it) }
    }

    override suspend fun searchProducts(query: String): List<Product> = withContext(Dispatchers.Default) {
        queries.searchProductsByName(query).executeAsList().map { assembleProduct(it) }
    }

    override suspend fun getProductCount(): Long = withContext(Dispatchers.Default) {
        queries.selectProductCount().executeAsOne()
    }

    override suspend fun getVariableProductIds(): List<Long> = withContext(Dispatchers.Default) {
        queries.selectVariableProductIds().executeAsList()
    }

    override suspend fun decrementStock(productId: Long, variantId: Long?, quantity: Int) =
        withContext(Dispatchers.Default) {
            if (variantId != null) {
                queries.decrementVariantStock(quantity.toLong(), variantId)
            } else {
                queries.decrementProductStock(quantity.toLong(), productId)
            }
        }

    override suspend fun saveProduct(product: Product) = withContext(Dispatchers.Default) {
        db.transaction {
            queries.insertOrReplaceProduct(
                id = product.id,
                name = product.name,
                sku = product.sku,
                barcode = product.barcode,
                price_cents = product.price.amountCents,
                regular_price_cents = product.regularPrice?.amountCents,
                sale_price_cents = product.salePrice?.amountCents,
                currency_code = product.price.currencyCode,
                stock_quantity = product.stockQuantity?.toLong(),
                manage_stock = if (product.manageStock) 1L else 0L,
                status = product.status.name,
                type = product.type.name,
                created_at = product.createdAt.toEpochMilliseconds(),
                updated_at = product.updatedAt.toEpochMilliseconds(),
                tax_class = product.taxClass,
            )

            queries.deleteVariantsByProductId(product.id)
            queries.deleteImagesByProductId(product.id)
            queries.deleteCategoryJoinsByProductId(product.id)
            queries.deleteTagJoinsByProductId(product.id)

            for (variant in product.variants) {
                queries.insertOrReplaceVariant(
                    id = variant.id,
                    product_id = product.id,
                    name = variant.name,
                    sku = variant.sku,
                    barcode = variant.barcode,
                    price_cents = variant.price.amountCents,
                    regular_price_cents = variant.regularPrice?.amountCents,
                    sale_price_cents = variant.salePrice?.amountCents,
                    currency_code = variant.price.currencyCode,
                    stock_quantity = variant.stockQuantity?.toLong(),
                    manage_stock = if (variant.manageStock) 1L else 0L,
                )
                queries.deleteAttributesByVariantId(variant.id)
                for (attr in variant.attributes) {
                    queries.insertVariantAttribute(
                        variant_id = variant.id,
                        name = attr.name,
                        value_ = attr.value,
                    )
                }
            }

            for (image in product.images) {
                queries.insertOrReplaceImage(
                    id = image.id,
                    product_id = product.id,
                    url = image.url,
                )
            }

            for (category in product.categories) {
                queries.insertOrReplaceCategory(id = category.id, name = category.name)
                    queries.updateCategoryName(name = category.name, id = category.id)
                queries.insertCategoryJoin(product_id = product.id, category_id = category.id)
            }

            for (tag in product.tags) {
                queries.insertOrReplaceTag(id = tag.id, name = tag.name)
                    queries.updateTagName(name = tag.name, id = tag.id)
                queries.insertTagJoin(product_id = product.id, tag_id = tag.id)
            }
        }
    }

    override suspend fun saveProducts(products: List<Product>) = withContext(Dispatchers.Default) {
        db.transaction {
            for (product in products) {
                queries.insertOrReplaceProduct(
                    id = product.id,
                    name = product.name,
                    sku = product.sku,
                    barcode = product.barcode,
                    price_cents = product.price.amountCents,
                    regular_price_cents = product.regularPrice?.amountCents,
                    sale_price_cents = product.salePrice?.amountCents,
                    currency_code = product.price.currencyCode,
                    stock_quantity = product.stockQuantity?.toLong(),
                    manage_stock = if (product.manageStock) 1L else 0L,
                    status = product.status.name,
                    type = product.type.name,
                    created_at = product.createdAt.toEpochMilliseconds(),
                    updated_at = product.updatedAt.toEpochMilliseconds(),
                    tax_class = product.taxClass,
                )

                queries.deleteVariantsByProductId(product.id)
                queries.deleteImagesByProductId(product.id)
                queries.deleteCategoryJoinsByProductId(product.id)
                queries.deleteTagJoinsByProductId(product.id)

                for (variant in product.variants) {
                    queries.insertOrReplaceVariant(
                        id = variant.id,
                        product_id = product.id,
                        name = variant.name,
                        sku = variant.sku,
                        barcode = variant.barcode,
                        price_cents = variant.price.amountCents,
                        regular_price_cents = variant.regularPrice?.amountCents,
                        sale_price_cents = variant.salePrice?.amountCents,
                        currency_code = variant.price.currencyCode,
                        stock_quantity = variant.stockQuantity?.toLong(),
                        manage_stock = if (variant.manageStock) 1L else 0L,
                    )
                    queries.deleteAttributesByVariantId(variant.id)
                    for (attr in variant.attributes) {
                        queries.insertVariantAttribute(
                            variant_id = variant.id,
                            name = attr.name,
                            value_ = attr.value,
                        )
                    }
                }

                for (image in product.images) {
                    queries.insertOrReplaceImage(id = image.id, product_id = product.id, url = image.url)
                }

                for (category in product.categories) {
                    queries.insertOrReplaceCategory(id = category.id, name = category.name)
                    queries.updateCategoryName(name = category.name, id = category.id)
                    queries.insertCategoryJoin(product_id = product.id, category_id = category.id)
                }

                for (tag in product.tags) {
                    queries.insertOrReplaceTag(id = tag.id, name = tag.name)
                    queries.updateTagName(name = tag.name, id = tag.id)
                    queries.insertTagJoin(product_id = product.id, tag_id = tag.id)
                }
            }
        }
    }

    override suspend fun deleteAllProducts() = withContext(Dispatchers.Default) {
        queries.deleteAllProducts()
    }

    // ========================================================================
    // Orders
    // ========================================================================

    override suspend fun saveOrder(order: Order): Long = withContext(Dispatchers.Default) {
        db.transactionWithResult {
            queries.insertOrder(
                remote_id = order.id.takeIf { it > 0 },
                order_number = order.number.ifEmpty { null },
                status = order.status.name,
                customer_id = order.customerId,
                total_cents = order.total.amountCents,
                total_tax_cents = order.totalTax.amountCents,
                currency_code = order.total.currencyCode,
                payment_method = order.paymentMethod.name,
                stripe_transaction_id = order.stripeTransactionId,
                idempotency_key = order.idempotencyKey,
                note = order.note,
                coupon_codes = order.couponCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
                created_at = order.createdAt.toEpochMilliseconds(),
                customer_email = order.customerEmail,
                payment_created_offline = if (order.paymentCreatedOffline) 1L else 0L,
            )
            val localId = queries.lastInsertOrderId().executeAsOne()

            for (item in order.lineItems) {
                queries.insertOrderLineItem(
                    order_id = localId,
                    product_id = item.productId,
                    variant_id = item.variantId,
                    name = item.name,
                    sku = item.sku,
                    quantity = item.quantity.toLong(),
                    unit_price_cents = item.unitPrice.amountCents,
                    total_price_cents = item.totalPrice.amountCents,
                    currency_code = item.unitPrice.currencyCode,
                )
            }
            for (fee in order.feeLines) {
                queries.insertOrderFeeLine(
                    order_id = localId,
                    name = fee.name,
                    amount_cents = fee.amount.amountCents,
                    currency_code = fee.amount.currencyCode,
                )
            }
            localId
        }
    }

    override suspend fun getPendingSyncOrders(): List<Order> = withContext(Dispatchers.Default) {
        queries.selectPendingSyncOrders().executeAsList().map { assembleOrder(it) }
    }

    override suspend fun getRecentOrders(limit: Int): List<Order> = withContext(Dispatchers.Default) {
        queries.selectRecentOrders(limit.toLong()).executeAsList().map { assembleOrder(it) }
    }

    override fun observePendingSyncOrderCount(): Flow<Long> =
        queries.selectPendingSyncOrders()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.size.toLong() }

    override suspend fun updateOrderStatus(localId: Long, status: OrderStatus) = withContext(Dispatchers.Default) {
        queries.updateOrderStatus(status = status.name, id = localId)
    }

    override suspend fun updateOrderStatusByIdempotencyKey(key: String, status: OrderStatus) = withContext(Dispatchers.Default) {
        queries.updateOrderStatusByIdempotencyKey(status = status.name, idempotency_key = key)
    }

    override suspend fun updateOrderRemoteId(localId: Long, remoteId: Long, orderNumber: String) =
        withContext(Dispatchers.Default) {
            queries.updateOrderRemoteId(
                remote_id = remoteId,
                order_number = orderNumber,
                id = localId,
            )
        }

    override suspend fun updateOrderStripeTransactionId(localId: Long, stripeTransactionId: String) =
        withContext(Dispatchers.Default) {
            queries.updateOrderStripeTransactionId(
                stripe_transaction_id = stripeTransactionId,
                id = localId,
            )
        }

    override suspend fun updateOrderCustomerEmail(localId: Long, email: String) =
        withContext(Dispatchers.Default) {
            queries.updateOrderCustomerEmail(
                customer_email = email,
                id = localId,
            )
        }

    override suspend fun getOrderByIdempotencyKey(key: String): Order? = withContext(Dispatchers.Default) {
        queries.selectOrderByIdempotencyKey(key).executeAsOneOrNull()?.let { assembleOrder(it) }
    }

    override suspend fun upsertRemoteOrder(order: Order): Long = withContext(Dispatchers.Default) {
        db.transactionWithResult {
            // Check if order exists locally by idempotency key
            val existing = if (order.idempotencyKey.isNotEmpty()) {
                queries.selectOrderByIdempotencyKey(order.idempotencyKey).executeAsOneOrNull()
            } else {
                null
            }
            // Also try by remote_id
            val existingByRemote = if (existing == null && order.id > 0) {
                queries.selectOrderByRemoteId(order.id).executeAsOneOrNull()
            } else {
                null
            }
            val match = existing ?: existingByRemote

            if (match != null) {
                // Don't overwrite a local PENDING_SYNC order — it hasn't been pushed yet
                if (match.status == OrderStatus.PENDING_SYNC.name) {
                    return@transactionWithResult match.id
                }

                // Update the existing row with remote data
                queries.updateOrderFromRemote(
                    status = order.status.name,
                    total_cents = order.total.amountCents,
                    total_tax_cents = order.totalTax.amountCents,
                    remote_id = order.id.takeIf { it > 0 },
                    order_number = order.number.ifEmpty { null },
                    stripe_transaction_id = order.stripeTransactionId,
                    note = order.note,
                    coupon_codes = order.couponCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
                    id = match.id,
                )

                // Replace line items
                queries.deleteLineItemsByOrderId(match.id)
                for (item in order.lineItems) {
                    queries.insertOrderLineItem(
                        order_id = match.id,
                        product_id = item.productId,
                        variant_id = item.variantId,
                        name = item.name,
                        sku = item.sku,
                        quantity = item.quantity.toLong(),
                        unit_price_cents = item.unitPrice.amountCents,
                        total_price_cents = item.totalPrice.amountCents,
                        currency_code = item.unitPrice.currencyCode,
                    )
                }
                // Replace fee lines
                queries.deleteFeeLinesByOrderId(match.id)
                for (fee in order.feeLines) {
                    queries.insertOrderFeeLine(
                        order_id = match.id,
                        name = fee.name,
                        amount_cents = fee.amount.amountCents,
                        currency_code = fee.amount.currencyCode,
                    )
                }
                match.id
            } else {
                // Insert as new row
                queries.insertOrder(
                    remote_id = order.id.takeIf { it > 0 },
                    order_number = order.number.ifEmpty { null },
                    status = order.status.name,
                    customer_id = order.customerId,
                    total_cents = order.total.amountCents,
                    total_tax_cents = order.totalTax.amountCents,
                    currency_code = order.total.currencyCode,
                    payment_method = order.paymentMethod.name,
                    stripe_transaction_id = order.stripeTransactionId,
                    idempotency_key = order.idempotencyKey,
                    note = order.note,
                    coupon_codes = order.couponCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
                    created_at = order.createdAt.toEpochMilliseconds(),
                    customer_email = order.customerEmail,
                    payment_created_offline = if (order.paymentCreatedOffline) 1L else 0L,
                )
                val localId = queries.lastInsertOrderId().executeAsOne()

                for (item in order.lineItems) {
                    queries.insertOrderLineItem(
                        order_id = localId,
                        product_id = item.productId,
                        variant_id = item.variantId,
                        name = item.name,
                        sku = item.sku,
                        quantity = item.quantity.toLong(),
                        unit_price_cents = item.unitPrice.amountCents,
                        total_price_cents = item.totalPrice.amountCents,
                        currency_code = item.unitPrice.currencyCode,
                    )
                }
                for (fee in order.feeLines) {
                    queries.insertOrderFeeLine(
                        order_id = localId,
                        name = fee.name,
                        amount_cents = fee.amount.amountCents,
                        currency_code = fee.amount.currencyCode,
                    )
                }
                localId
            }
        }
    }

    override fun observeRecentOrders(limit: Int): Flow<List<Order>> =
        queries.selectRecentOrders(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { assembleOrder(it) } }

    override suspend fun markOrderRefunded(remoteId: Long, refundedAt: Instant, stripeRefundId: String?) =
        withContext(Dispatchers.Default) {
            queries.updateOrderRefundStatus(
                refunded_at = refundedAt.toEpochMilliseconds(),
                stripe_refund_id = stripeRefundId,
                remote_id = remoteId,
            )
        }

    // ========================================================================
    // Tax Rates
    // ========================================================================

    override suspend fun saveTaxRates(rates: List<TaxRate>) = withContext(Dispatchers.Default) {
        db.transaction {
            queries.deleteAllTaxRates()
            for (rate in rates) {
                queries.insertOrReplaceTaxRate(
                    id = rate.id,
                    name = rate.name,
                    rate = rate.rate,
                    country = rate.country,
                    state = rate.state,
                    is_compound = if (rate.isCompound) 1L else 0L,
                    is_shipping = if (rate.isShipping) 1L else 0L,
                    tax_class = rate.taxClass,
                )
            }
        }
    }

    override suspend fun getAllTaxRates(): List<TaxRate> = withContext(Dispatchers.Default) {
        queries.selectAllTaxRates().executeAsList().map {
            TaxRate(
                id = it.id,
                name = it.name,
                rate = it.rate,
                country = it.country,
                state = it.state,
                isCompound = it.is_compound != 0L,
                isShipping = it.is_shipping != 0L,
                taxClass = it.tax_class,
            )
        }
    }

    // ========================================================================
    // Sync State
    // ========================================================================

    override suspend fun getLastSyncedAt(entityType: String): Instant? = withContext(Dispatchers.Default) {
        queries.selectSyncState(entityType).executeAsOneOrNull()
            ?.let { Instant.fromEpochMilliseconds(it.last_synced_at) }
    }

    override suspend fun updateLastSyncedAt(entityType: String, timestamp: Instant) =
        withContext(Dispatchers.Default) {
            queries.upsertSyncState(entityType, timestamp.toEpochMilliseconds())
        }

    // ========================================================================
    // Store Config
    // ========================================================================

    override fun observeStoreConfig(): Flow<StoreConfig?> =
        queries.selectStoreConfig()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.let { row -> mapStoreConfigRow(row) } }

    override suspend fun getStoreConfig(): StoreConfig? = withContext(Dispatchers.Default) {
        queries.selectStoreConfig().executeAsOneOrNull()?.let { mapStoreConfigRow(it) }
    }

    override suspend fun saveStoreConfig(config: StoreConfig) = withContext(Dispatchers.Default) {
        queries.upsertStoreConfig(
            site_url = config.siteUrl,
            consumer_key = config.consumerKey,
            consumer_secret = config.consumerSecret,
            currency = config.currency,
            register_name = config.registerName,
        )
    }

    override suspend fun deleteStoreConfig() = withContext(Dispatchers.Default) {
        queries.deleteStoreConfig()
    }

    // ========================================================================
    // Private: Row → Domain Assembly
    // ========================================================================

    private fun assembleProduct(row: Product_cache): Product {
        val variants = queries.selectVariantsByProductId(row.id).executeAsList().map { v ->
            val attrs = queries.selectAttributesByVariantId(v.id).executeAsList().map { a ->
                VariantAttribute(name = a.name, value = a.value_)
            }
            ProductVariant(
                id = v.id,
                productId = v.product_id,
                name = v.name,
                sku = v.sku,
                barcode = v.barcode,
                price = Money(v.price_cents, v.currency_code),
                regularPrice = v.regular_price_cents?.let { Money(it, v.currency_code) },
                salePrice = v.sale_price_cents?.let { Money(it, v.currency_code) },
                stockQuantity = v.stock_quantity?.toInt(),
                manageStock = v.manage_stock != 0L,
                attributes = attrs,
            )
        }

        val images = queries.selectImagesByProductId(row.id).executeAsList().map {
            ProductImage(id = it.id, url = it.url)
        }

        val categories = queries.selectCategoriesByProductId(row.id).executeAsList().map {
            ProductCategory(id = it.id, name = it.name)
        }

        val tags = queries.selectTagsByProductId(row.id).executeAsList().map {
            ProductTag(id = it.id, name = it.name)
        }

        return Product(
            id = row.id,
            name = row.name,
            sku = row.sku,
            barcode = row.barcode,
            price = Money(row.price_cents, row.currency_code),
            regularPrice = row.regular_price_cents?.let { Money(it, row.currency_code) },
            salePrice = row.sale_price_cents?.let { Money(it, row.currency_code) },
            stockQuantity = row.stock_quantity?.toInt(),
            manageStock = row.manage_stock != 0L,
            status = ProductStatus.valueOf(row.status),
            images = images,
            categories = categories,
            tags = tags,
            variants = variants,
            type = ProductType.valueOf(row.type),
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
            taxClass = row.tax_class,
        )
    }

    private fun assembleOrder(row: Offline_orders): Order {
        val lineItems = queries.selectLineItemsByOrderId(row.id).executeAsList().map { li ->
            LineItem(
                id = li.id,
                productId = li.product_id,
                variantId = li.variant_id,
                name = li.name,
                sku = li.sku,
                quantity = li.quantity.toInt(),
                unitPrice = Money(li.unit_price_cents, li.currency_code),
                totalPrice = Money(li.total_price_cents, li.currency_code),
            )
        }

        val feeLines = queries.selectFeeLinesByOrderId(row.id).executeAsList().map { fl ->
            FeeLine(
                name = fl.name,
                amount = Money(fl.amount_cents, fl.currency_code),
            )
        }

        return Order(
            id = row.remote_id ?: row.id,
            number = row.order_number ?: "",
            status = OrderStatus.valueOf(row.status),
            lineItems = lineItems,
            feeLines = feeLines,
            customerId = row.customer_id,
            total = Money(row.total_cents, row.currency_code),
            totalTax = Money(row.total_tax_cents, row.currency_code),
            paymentMethod = PaymentMethod.valueOf(row.payment_method),
            stripeTransactionId = row.stripe_transaction_id,
            idempotencyKey = row.idempotency_key,
            note = row.note,
            couponCodes = row.coupon_codes?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            customerEmail = row.customer_email,
            paymentCreatedOffline = row.payment_created_offline != 0L,
        )
    }

    // ========================================================================
    // Staff Users
    // ========================================================================

    override fun observeStaffUsers(): Flow<List<StaffUser>> =
        queries.selectAllStaffUsers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    StaffUser(
                        id = it.id,
                        firstName = it.first_name,
                        lastName = it.last_name,
                        pinSha256 = it.pin_sha256,
                    )
                }
            }

    override suspend fun getStaffUsers(): List<StaffUser> = withContext(Dispatchers.Default) {
        queries.selectAllStaffUsers().executeAsList().map {
            StaffUser(
                id = it.id,
                firstName = it.first_name,
                lastName = it.last_name,
                pinSha256 = it.pin_sha256,
            )
        }
    }

    override suspend fun saveStaffUsers(users: List<StaffUser>) = withContext(Dispatchers.Default) {
        db.transaction {
            queries.deleteAllStaffUsers()
            for (user in users) {
                queries.insertOrReplaceStaffUser(
                    id = user.id,
                    first_name = user.firstName,
                    last_name = user.lastName,
                    pin_sha256 = user.pinSha256,
                )
            }
        }
    }

    // ========================================================================
    // Device ID
    // ========================================================================

    override suspend fun getDeviceId(): String? = withContext(Dispatchers.Default) {
        queries.selectAppSetting("device_id").executeAsOneOrNull()
    }

    override suspend fun saveDeviceId(deviceId: String) = withContext(Dispatchers.Default) {
        queries.upsertAppSetting("device_id", deviceId)
    }

    // ========================================================================
    // Offline Payment Config
    // ========================================================================

    override suspend fun getOfflinePaymentConfig(): OfflinePaymentConfig = withContext(Dispatchers.Default) {
        val enabled = queries.selectAppSetting("offline_payments_enabled").executeAsOneOrNull() == "true"
        val perTx = queries.selectAppSetting("offline_per_transaction_limit_cents").executeAsOneOrNull()?.toLongOrNull() ?: 0L
        val total = queries.selectAppSetting("offline_total_limit_cents").executeAsOneOrNull()?.toLongOrNull() ?: 0L
        OfflinePaymentConfig(enabled = enabled, perTransactionLimitCents = perTx, totalLimitCents = total)
    }

    override suspend fun saveOfflinePaymentConfig(config: OfflinePaymentConfig) = withContext(Dispatchers.Default) {
        db.transaction {
            queries.upsertAppSetting("offline_payments_enabled", config.enabled.toString())
            queries.upsertAppSetting("offline_per_transaction_limit_cents", config.perTransactionLimitCents.toString())
            queries.upsertAppSetting("offline_total_limit_cents", config.totalLimitCents.toString())
        }
    }

    override fun observeOfflinePaymentEnabled(): Flow<Boolean> =
        queries.selectAppSetting("offline_payments_enabled")
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it == "true" }

    // ========================================================================
    // Consent Audit Log
    // ========================================================================

    override suspend fun logOfflinePaymentConsent(entry: ConsentLogEntry) = withContext(Dispatchers.Default) {
        queries.insertConsentLogEntry(
            device_id = entry.deviceId,
            action = entry.action.name,
            per_transaction_limit_cents = entry.perTransactionLimitCents,
            total_limit_cents = entry.totalLimitCents,
            risk_text_version = entry.riskTextVersion,
            created_at = entry.createdAt.toEpochMilliseconds(),
        )
    }

    override suspend fun getConsentLog(): List<ConsentLogEntry> = withContext(Dispatchers.Default) {
        queries.selectAllConsentLogEntries().executeAsList().map {
            ConsentLogEntry(
                id = it.id,
                deviceId = it.device_id,
                action = ConsentAction.valueOf(it.action),
                perTransactionLimitCents = it.per_transaction_limit_cents,
                totalLimitCents = it.total_limit_cents,
                riskTextVersion = it.risk_text_version,
                createdAt = Instant.fromEpochMilliseconds(it.created_at),
            )
        }
    }

    // ========================================================================
    // Offline Order Tracking
    // ========================================================================

    override suspend fun getUnreconciledOfflineOrders(): List<Order> = withContext(Dispatchers.Default) {
        queries.selectUnreconciledOfflineOrders().executeAsList().map { assembleOrder(it) }
    }

    // ========================================================================
    // Private: Row → Domain Assembly
    // ========================================================================

    private fun mapStoreConfigRow(row: Store_config): StoreConfig = StoreConfig(
        siteUrl = row.site_url,
        consumerKey = row.consumer_key,
        consumerSecret = row.consumer_secret,
        currency = row.currency,
        registerName = row.register_name,
    )
}
