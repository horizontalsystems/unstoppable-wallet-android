package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

data class CoinGeckoCoinResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val platforms: Map<String, String>,
    val tickers: List<MarketTickerRaw>
) {

    private fun isSmartContractAddress(v: String): Boolean {
        return v.matches("^0[xX][A-z0-9]+$".toRegex())
    }

    private fun coinCode(coins: List<Coin>, coinId: String): String? {
        return coins.firstOrNull { it.uid == coinId }?.code
    }

    val exchangeIds: List<String>
        get() = tickers.map { it.market.id }

    fun marketTickers(imageUrls: Map<String, String>, coins: List<Coin>): List<MarketTicker> {
        val contractAddresses = platforms.mapNotNull { (platformName, contractAddress) ->
            if (smartContractPlatforms.contains(platformName)) {
                contractAddress.lowercase()
            } else {
                null
            }
        }

        val updatedTickers = tickers.mapNotNull { raw ->
            if(raw.lastRate.compareTo(BigDecimal.ZERO) == 0  || raw.volume.compareTo(BigDecimal.ZERO) == 0) {
                return@mapNotNull null
            }

            var base = if (contractAddresses.contains(raw.base.lowercase(Locale.ENGLISH))) {
                symbol.uppercase()
            } else {
                raw.base
            }

            var target = if (contractAddresses.contains(raw.target.lowercase(Locale.ENGLISH))) {
                symbol.uppercase()
            } else {
                raw.target
            }

            if (isSmartContractAddress(base)) {
                val coinCode = coinCode(coins, raw.coinId)
                if (coinCode != null) {
                    base = coinCode.uppercase()
                } else {
                    return@mapNotNull null
                }
            }

            if (isSmartContractAddress(target)) {
                val coinCode = raw.targetCoinId?.let { coinCode(coins, it) }

                if (coinCode != null) {
                    target = coinCode.uppercase()
                } else {
                    return@mapNotNull null
                }
            }

            MarketTickerRaw(
                raw.coinId,
                base,
                target,
                raw.market,
                raw.lastRate,
                raw.volume,
                raw.targetCoinId,
                raw.tradeUrl
            )
        }

        return updatedTickers.map {
            val imageUrl = imageUrls[it.market.id]
                var target = it.target
                var base = it.base
                var volume = it.volume
                var lastRate = it.lastRate
                if (it.target.lowercase() == symbol.lowercase()) {
                    base = symbol.uppercase()
                    target = it.base
                    volume *= lastRate
                    lastRate = BigDecimal.ONE.divide(lastRate, 4, RoundingMode.HALF_EVEN)
                }
                MarketTicker(
                    base,
                    target,
                    it.market.name,
                    imageUrl,
                    lastRate,
                    volume,
                    it.tradeUrl
                )
        }
    }

    companion object {
        private val smartContractPlatforms: List<String> =
            listOf("tron", "ethereum", "eos", "binance-smart-chain", "binancecoin")
    }
}

data class MarketTickerRaw(
    @SerializedName("coin_id")
    val coinId: String,
    val base: String,
    val target: String,
    val market: TickerMarketRaw,
    @SerializedName("last")
    val lastRate: BigDecimal,
    val volume: BigDecimal,
    @SerializedName("target_coin_id")
    val targetCoinId: String?,
    @SerializedName("trade_url")
    val tradeUrl: String?,
)

data class TickerMarketRaw(
    @SerializedName("identifier")
    val id: String,
    val name: String,
)
