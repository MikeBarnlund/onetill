package com.onetill.shared.ecommerce.woocommerce

import com.onetill.shared.data.model.StoreConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createWooCommerceHttpClient(config: StoreConfig): HttpClient {
    val baseUrl = config.siteUrl.trimEnd('/')

    return HttpClient {
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

        install(Logging) {
            level = LogLevel.NONE
        }

        defaultRequest {
            url {
                val parsed = io.ktor.http.Url(baseUrl)
                protocol = parsed.protocol
                host = parsed.host
                if (parsed.specifiedPort != 0 && parsed.specifiedPort != protocol.defaultPort) {
                    port = parsed.specifiedPort
                }
                path("wp-json/wc/v3/")
            }
        }
    }
}
