package cash.p.terminal.network.pirate.api

import cash.p.terminal.network.api.parseResponse
import cash.p.terminal.network.data.entity.ChartPeriod
import cash.p.terminal.network.pirate.data.entity.CalculatorDataDto
import cash.p.terminal.network.pirate.data.entity.ChangeNowAssociatedCoinDto
import cash.p.terminal.network.pirate.data.entity.InvestmentDataDto
import cash.p.terminal.network.pirate.data.entity.InvestmentGraphDataDto
import cash.p.terminal.network.pirate.data.entity.MarketTickerDto
import cash.p.terminal.network.pirate.data.entity.PiratePlaceCoinDto
import cash.p.terminal.network.pirate.data.entity.StakeDataDto
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

    suspend fun getCoinInfo(coin: String): PiratePlaceCoinDto {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "coins/$coin")
        }.parseResponse()
    }

    suspend fun getCoinPriceChart(
        coin: String,
        periodType: ChartPeriod
    ): List<List<String>> {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "coins/$coin/graph/usd/${periodType.value}")
        }.parseResponse()
    }

    suspend fun getMarketTickers(
        coin: String
    ): List<MarketTickerDto> {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "coins/$coin/tickers")
        }.parseResponse()
    }

    suspend fun getInvestmentData(coin: String, address: String): InvestmentDataDto {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "invest/$coin/$address")
        }.parseResponse()
    }

    suspend fun getChangeNowCoinAssociation(uid: String): List<ChangeNowAssociatedCoinDto> {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "changenow/$uid")
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