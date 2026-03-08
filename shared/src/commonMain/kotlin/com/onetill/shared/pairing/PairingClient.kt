package com.onetill.shared.pairing

import com.onetill.shared.data.AppResult
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PairingRequest(
    @SerialName("token") val token: String,
    @SerialName("nonce") val nonce: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class PairingResponse(
    @SerialName("credentials") val credentials: PairingCredentials,
    @SerialName("store") val store: PairingStoreInfo,
)

@Serializable
data class PairingCredentials(
    @SerialName("consumer_key") val consumerKey: String,
    @SerialName("consumer_secret") val consumerSecret: String,
)

@Serializable
data class PairingStoreInfo(
    @SerialName("name") val name: String,
    @SerialName("currency") val currency: String,
)

class PairingClient {

    private val client = HttpClient {
        expectSuccess = false

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 15_000
        }

        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.d(message, tag = "PairingHTTP")
                }
            }
        }
    }

    suspend fun completePairing(
        storeUrl: String,
        request: PairingRequest,
    ): AppResult<PairingResponse> {
        return try {
            val url = "${storeUrl.trimEnd('/')}/wp-json/onetill/v1/pair/complete"
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status.value) {
                200 -> AppResult.Success(response.body<PairingResponse>())
                401, 403 -> AppResult.Error("Pairing token is invalid or expired. Generate a new QR code in WP Admin.")
                404 -> AppResult.Error("OneTill plugin not found. Make sure it's installed and activated.")
                410 -> AppResult.Error("This QR code has already been used. Generate a new one in WP Admin.")
                429 -> AppResult.Error("Too many attempts. Wait a moment and try again.")
                else -> AppResult.Error("Pairing failed (${response.status.value}). Try generating a new QR code.")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Network error during pairing", e)
        }
    }
}
