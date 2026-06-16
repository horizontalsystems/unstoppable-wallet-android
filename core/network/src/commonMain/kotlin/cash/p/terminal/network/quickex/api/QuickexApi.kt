package cash.p.terminal.network.quickex.api

import cash.p.terminal.network.data.setJsonBody
import cash.p.terminal.network.quickex.data.entity.BackendQuickexResponseError
import cash.p.terminal.network.quickex.data.entity.NewTransactionQuickexResponseDto
import cash.p.terminal.network.quickex.data.entity.QuickexInstrumentDto
import cash.p.terminal.network.quickex.data.entity.QuickexRatesDto
import cash.p.terminal.network.quickex.data.entity.TransactionQuickexStatusDto
import cash.p.terminal.network.quickex.data.entity.request.NewTransactionQuickexRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import java.math.BigDecimal

internal class QuickexApi(
    private val httpClient: HttpClient
) {
    private companion object Companion {
        const val BASE_URL = "https://quickex.io/api/"
        const val REFERRER_ID = "aff_3079"
    }

    suspend fun getAvailableCurrencies(): List<QuickexInstrumentDto> {
        return httpClient.get {
            url(BASE_URL + "v1/instruments/public")
        }.parseResponse()
    }

    suspend fun getRates(
        fromCurrency: String,
        fromNetwork: String,
        toCurrency: String,
        toNetwork: String,
        claimedDepositAmount: BigDecimal
    ): QuickexRatesDto {
        return httpClient.get {
            url(BASE_URL + "v1/rates/public/one")
            parameter("exchangeType", "crypto")
            parameter("instrumentFromCurrencyTitle", fromCurrency)
            parameter("instrumentFromNetworkTitle", fromNetwork)
            parameter("instrumentToCurrencyTitle", toCurrency)
            parameter("instrumentToNetworkTitle", toNetwork)
            parameter("claimedDepositAmountCurrency", fromCurrency)
            parameter("claimedDepositAmount", claimedDepositAmount.toPlainString())
            parameter("referrerId", REFERRER_ID)
        }.parseResponse()
    }

    suspend fun createTransaction(
        newTransactionRequest: NewTransactionQuickexRequest
    ): NewTransactionQuickexResponseDto {
        return httpClient.post {
            url(BASE_URL + "v1/orders/public/create")
            setJsonBody(newTransactionRequest.withReferrerId(REFERRER_ID))
        }.parseResponse()
    }

    suspend fun getTransactionStatus(
        destinationAddress: String,
        orderId: String
    ): TransactionQuickexStatusDto {
        return httpClient.get {
            url(BASE_URL + "v1/orders/public-info")
            parameter("destinationAddress", destinationAddress)
            parameter("orderId", orderId)
        }.parseResponse()
    }
}

internal suspend inline fun <reified T : Any> HttpResponse.parseResponse(): T {
    return if (status.isSuccess()) {
        body<T>()
    } else {
        throw body<BackendQuickexResponseError>()
    }
}