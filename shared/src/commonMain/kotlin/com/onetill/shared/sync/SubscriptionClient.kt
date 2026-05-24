package com.onetill.shared.sync

import com.onetill.shared.ecommerce.woocommerce.dto.SubscriptionDto
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RegisterTrialRequest(val store_url: String)

class SubscriptionClient {

    private val client = HttpClient {
        expectSuccess = false

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 10_000
        }
    }

    suspend fun registerTrial(storeUrl: String): SubscriptionDto? {
        return try {
            val response = client.post("${SupabaseConfig.BASE_URL}/functions/v1/register-trial") {
                header("Authorization", "Bearer ${SupabaseConfig.API_KEY}")
                contentType(ContentType.Application.Json)
                setBody(RegisterTrialRequest(storeUrl))
            }
            if (response.status.value == 200) {
                response.body<SubscriptionDto>()
            } else {
                Napier.w("register-trial returned ${response.status.value}")
                null
            }
        } catch (e: Exception) {
            Napier.w("register-trial failed: ${e.message}")
            null
        }
    }

    suspend fun checkSubscription(storeUrl: String): SubscriptionDto? {
        return try {
            val response = client.get("${SupabaseConfig.BASE_URL}/functions/v1/check-subscription") {
                header("Authorization", "Bearer ${SupabaseConfig.API_KEY}")
                parameter("store_url", storeUrl)
            }
            if (response.status.value == 200) {
                response.body<SubscriptionDto>()
            } else {
                Napier.w("check-subscription returned ${response.status.value}")
                null
            }
        } catch (e: Exception) {
            Napier.w("check-subscription failed: ${e.message}")
            null
        }
    }
}
