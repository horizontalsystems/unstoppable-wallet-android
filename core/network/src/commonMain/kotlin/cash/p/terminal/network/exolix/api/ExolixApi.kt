package cash.p.terminal.network.exolix.api

import cash.p.terminal.network.data.EncodedSecrets
import cash.p.terminal.network.data.setJsonBody
import cash.p.terminal.network.exolix.data.entity.BackendExolixResponseError
import cash.p.terminal.network.exolix.data.entity.ExolixNetworkDto
import cash.p.terminal.network.exolix.data.entity.ExolixRateDto
import cash.p.terminal.network.exolix.data.entity.ExolixTransactionDto
import cash.p.terminal.network.exolix.data.entity.request.NewTransactionExolixRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.math.BigDecimal

internal class ExolixApi(
    private val httpClient: HttpClient
) {
    private companion object {
        const val BASE_URL = "https://exolix.com/api/v2/"
    }

    suspend fun getCurrencyNetworks(code: String): List<ExolixNetworkDto> {
        return httpClient.get {
            url(BASE_URL + "currencies/$code/networks")
            authorize()
            accept(ContentType.Application.Json)
        }.parseExolixResponse()
    }

    suspend fun getRate(
        coinFrom: String,
        networkFrom: String,
        coinTo: String,
        networkTo: String,
        amount: BigDecimal,
        rateType: String,
    ): ExolixRateDto {
        return httpClient.get {
            url(BASE_URL + "rate")
            authorize()
            accept(ContentType.Application.Json)
            parameter("coinFrom", coinFrom)
            parameter("networkFrom", networkFrom)
            parameter("coinTo", coinTo)
            parameter("networkTo", networkTo)
            parameter("amount", amount.toPlainString())
            parameter("rateType", rateType)
        }.parseExolixResponse()
    }

    suspend fun createTransaction(
        newTransactionRequest: NewTransactionExolixRequest
    ): ExolixTransactionDto {
        return httpClient.post {
            url(BASE_URL + "transactions")
            authorize()
            accept(ContentType.Application.Json)
            setJsonBody(newTransactionRequest)
        }.parseExolixResponse()
    }

    suspend fun getTransaction(
        transactionId: String
    ): ExolixTransactionDto {
        return httpClient.get {
            url(BASE_URL + "transactions/$transactionId")
            authorize()
            accept(ContentType.Application.Json)
        }.parseExolixResponse()
    }

    private fun HttpRequestBuilder.authorize() {
        header("api-key", EncodedSecrets.EXOLIX_API_KEY)
    }
}

internal suspend inline fun <reified T : Any> HttpResponse.parseExolixResponse(): T {
    return if (status.isSuccess()) {
        body<T>()
    } else {
        throw body<BackendExolixResponseError>()
    }
}
