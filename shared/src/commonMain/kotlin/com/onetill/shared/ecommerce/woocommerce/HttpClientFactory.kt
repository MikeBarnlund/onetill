package com.onetill.shared.ecommerce.woocommerce

import com.onetill.shared.data.model.StoreConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpTimeout
import io.github.aakira.napier.Napier
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createOneTillPluginHttpClient(config: StoreConfig): HttpClient =
    createAuthenticatedHttpClient(config, "wp-json/onetill/v1/")

fun createWooCommerceHttpClient(config: StoreConfig): HttpClient =
    createAuthenticatedHttpClient(config, "wp-json/wc/v3/")

private fun createAuthenticatedHttpClient(config: StoreConfig, apiPath: String): HttpClient {
    val baseUrl = config.siteUrl.trimEnd('/')

    return HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Auth) {
            basic {
                sendWithoutRequest { true }
                credentials {
                    BasicAuthCredentials(
                        username = config.consumerKey,
                        password = config.consumerSecret,
                    )
                }
            }
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
                    Napier.d(message, tag = "WooHTTP")
                }
            }
        }

        defaultRequest {
            headers.append("ngrok-skip-browser-warning", "true")
            url {
                val parsed = io.ktor.http.Url(baseUrl)
                protocol = parsed.protocol
                host = parsed.host
                if (parsed.specifiedPort != 0 && parsed.specifiedPort != protocol.defaultPort) {
                    port = parsed.specifiedPort
                }
                path(apiPath)
            }
        }
    }
}
