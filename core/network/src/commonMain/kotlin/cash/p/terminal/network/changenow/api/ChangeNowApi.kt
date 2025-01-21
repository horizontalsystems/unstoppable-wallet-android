package cash.p.terminal.network.changenow.api

import cash.p.terminal.network.api.parseChangeNowResponse
import cash.p.terminal.network.changenow.data.entity.ChangeNowCurrencyDto
import cash.p.terminal.network.changenow.data.entity.ExchangeAmountDto
import cash.p.terminal.network.changenow.data.entity.MinAmountDto
import cash.p.terminal.network.changenow.data.entity.NewTransactionResponseDto
import cash.p.terminal.network.changenow.data.entity.request.NewTransactionRequest
import cash.p.terminal.network.data.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import java.math.BigDecimal

internal class ChangeNowApi(
    private val httpClient: HttpClient
) {
    private companion object {
        const val BASE_URL = "https://api.changenow.io/v1/"
        const val API_KEY = "8d648ad70417a34390286511c2799235438c4464203732d98226c45f53364eba"
    }

    suspend fun getAvailableCurrencies(): List<ChangeNowCurrencyDto> {
        return httpClient.get {
            url(BASE_URL + "currencies?active=true")
        }.parseChangeNowResponse()
    }

    suspend fun getExchangeAmount(
        tickerFrom: String,
        tickerTo: String,
        amount: BigDecimal
    ): ExchangeAmountDto {
        return httpClient.get {
            amount.toPlainString()
            url(BASE_URL + "exchange-amount/${amount.toPlainString()}/${tickerFrom}_${tickerTo}")
            parameter("api_key", API_KEY)
        }.parseChangeNowResponse()
    }

    suspend fun getMinAmount(
        tickerFrom: String,
        tickerTo: String
    ): MinAmountDto {
        return httpClient.get {
            url(BASE_URL + "min-amount/${tickerFrom}_${tickerTo}")
            parameter("api_key", API_KEY)
        }.parseChangeNowResponse()
    }

    suspend fun createTransaction(
        newTransactionRequest: NewTransactionRequest
    ): NewTransactionResponseDto {
        return httpClient.post {
            url(BASE_URL + "transactions/$API_KEY")
            setJsonBody(newTransactionRequest)
        }.parseChangeNowResponse()
    }
}