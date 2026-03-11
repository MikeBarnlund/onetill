package com.onetill.shared.data.model

data class DailySummary(
    val totalSalesCents: Long,
    val currency: String,
    val transactionCount: Int,
    val averageOrderCents: Long,
    val cardPaymentsCents: Long,
    val cashPaymentsCents: Long,
    val itemsSold: Int,
    val pendingSyncCount: Int,
)
