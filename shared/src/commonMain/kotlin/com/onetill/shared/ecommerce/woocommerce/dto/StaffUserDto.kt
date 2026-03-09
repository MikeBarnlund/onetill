package com.onetill.shared.ecommerce.woocommerce.dto

import com.onetill.shared.data.model.StaffUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StaffUserDto(
    val id: Long,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("pin_sha256") val pinSha256: String? = null,
)

fun StaffUserDto.toDomain(): StaffUser = StaffUser(
    id = id,
    firstName = firstName,
    lastName = lastName,
    pinSha256 = pinSha256,
)
