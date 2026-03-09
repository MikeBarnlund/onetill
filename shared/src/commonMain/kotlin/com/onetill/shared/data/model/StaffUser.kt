package com.onetill.shared.data.model

data class StaffUser(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val pinSha256: String?,
)
