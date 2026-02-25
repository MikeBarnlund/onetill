package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
)

@Serializable
data class CustomerDraft(
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
)
