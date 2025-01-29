package cash.p.terminal.network.data

import cash.p.terminal.network.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun buildNetworkClient() = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    if (BuildConfig.DEBUG) {
        install(Logging) {
            logger = object : Logger {
                private val logTag = "KtorHttpLogger: "
                override fun log(message: String) {
                    println(logTag + message)
                }
            }
        }
    }
}

internal inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
    header(HttpHeaders.ContentType, ContentType.Application.Json)
    setBody(body)
}