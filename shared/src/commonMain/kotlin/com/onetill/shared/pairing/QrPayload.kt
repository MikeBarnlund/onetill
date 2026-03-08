package com.onetill.shared.pairing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class QrPayload(
    @SerialName("v") val version: Int,
    @SerialName("store_url") val storeUrl: String,
    @SerialName("token") val token: String,
    @SerialName("nonce") val nonce: String,
    @SerialName("plugin_version") val pluginVersion: String,
)

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private const val QR_PREFIX = "onetill://pair?d="

@OptIn(ExperimentalEncodingApi::class)
fun parseQrCode(raw: String): QrPayload? {
    if (!raw.startsWith(QR_PREFIX)) return null
    val encoded = raw.removePrefix(QR_PREFIX)
    return try {
        // PHP strips base64 padding — add it back
        val padded = when (encoded.length % 4) {
            2 -> encoded + "=="
            3 -> encoded + "="
            else -> encoded
        }
        val decoded = Base64.UrlSafe.decode(padded).decodeToString()
        lenientJson.decodeFromString<QrPayload>(decoded)
    } catch (_: Exception) {
        null
    }
}
