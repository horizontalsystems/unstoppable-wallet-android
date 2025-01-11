package cash.p.terminal.network.api

import cash.p.terminal.network.data.entity.CalculatorDataDto
import cash.p.terminal.network.data.entity.InvestmentDataDto
import cash.p.terminal.network.data.entity.InvestmentGraphDataDto
import cash.p.terminal.network.data.entity.StakeDataDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import java.util.Locale

internal class PlaceApi(
    private val httpClient: HttpClient
) {
    private companion object {
        const val PIRATE_BASE_PLACE_URL = "https://pirate.place/api/"
    }

    suspend fun getInvestmentData(coin: String, address: String): InvestmentDataDto {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "invest/$coin/$address")
        }.parseResponse()
    }

    suspend fun getInvestmentChart(
        coin: String,
        address: String,
        period: String
    ): InvestmentGraphDataDto {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "invest/$coin/$address/graph/$period")
        }.parseResponse()
    }

    suspend fun getStakeData(coin: String, address: String): StakeDataDto {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "invest/$coin/$address/stake")
        }.parseResponse()
    }

    suspend fun getCalculatorData(coin: String, amount: Double): CalculatorDataDto {
        return httpClient.get {
            val formattedAmount = String.format(Locale.US, "%s", amount)
            url(PIRATE_BASE_PLACE_URL + "invest/$coin/calculator")
            parameter("amount", formattedAmount)
        }.parseResponse()
    }
}