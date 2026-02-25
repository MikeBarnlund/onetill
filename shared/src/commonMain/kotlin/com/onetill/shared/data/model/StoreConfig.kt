package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreConfig(
    val siteUrl: String,
    val consumerKey: String,
    val consumerSecret: String,
    val currency: String,
)
