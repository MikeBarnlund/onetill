package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TaxRate(
    val id: Long,
    val name: String,
    val rate: String,
    val country: String,
    val state: String,
    val isCompound: Boolean,
    val isShipping: Boolean,
)
