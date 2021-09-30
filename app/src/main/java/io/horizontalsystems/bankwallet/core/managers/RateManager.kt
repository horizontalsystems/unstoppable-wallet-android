package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.coinkit.models.CoinType as CoinKitCoinType
import io.horizontalsystems.xrateskit.XRatesKit
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal

class RateManager(
        context: Context,
        private val appConfigProvider: IAppConfigProvider) : IRateManager {

    private val kit: XRatesKit by lazy {
        XRatesKit.create(
                context,
                rateExpirationInterval = 60 * 10,
                cryptoCompareApiKey = appConfigProvider.cryptoCompareApiKey,
                defiyieldProviderApiKey = appConfigProvider.defiyieldProviderApiKey,
                coinsRemoteUrl = appConfigProvider.coinsJsonUrl,
                providerCoinsRemoteUrl = appConfigProvider.providerCoinsJsonUrl,
        )
    }

    override fun historicalRateCached(coinType: CoinType, currencyCode: String, timestamp: Long): BigDecimal? {
        return kit.getHistoricalRate(coinType.toCoinKitCoinType(), currencyCode, timestamp)
    }

    override fun historicalRate(coinType: CoinType, currencyCode: String, timestamp: Long): Single<BigDecimal> {
        return kit.getHistoricalRate(coinType.toCoinKitCoinType(), currencyCode, timestamp)?.let { Single.just(it) }
                ?: kit.getHistoricalRateAsync(coinType.toCoinKitCoinType(), currencyCode, timestamp)
    }

    override fun chartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType): ChartInfo? {
        return kit.getChartInfo(coinType.toCoinKitCoinType(), currencyCode, chartType)
    }

    override fun chartInfoObservable(coinType: CoinType, currencyCode: String, chartType: ChartType): Observable<ChartInfo> {
        return kit.chartInfoObservable(coinType.toCoinKitCoinType(), currencyCode, chartType)
    }

    override fun coinMarketDetailsAsync(coinType: CoinType, currencyCode: String, rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>): Single<CoinMarketDetails> {
        return kit.getCoinMarketDetailsAsync(coinType.toCoinKitCoinType(), currencyCode, rateDiffCoinCodes, rateDiffPeriods)
    }

    override fun getTopTokenHoldersAsync(coinType: CoinType): Single<List<TokenHolder>> {
        return kit.getTopTokenHoldersAsync(coinType.toCoinKitCoinType(), itemsCount = 10)
    }

    override fun getAuditsAsync(coinType: CoinType): Single<List<Auditor>> {
        return kit.getAuditReportsAsync(coinType.toCoinKitCoinType())
    }

    override fun getTopDefiTvlAsync(currencyCode: String, fetchDiffPeriod: TimePeriod, itemsCount: Int, chain: String?): Single<List<DefiTvl>> {
        return kit.getTopDefiTvlAsync(currencyCode, fetchDiffPeriod, itemsCount, chain)
    }

    override fun getGlobalMarketInfoAsync(currency: String): Single<GlobalCoinMarket> {
        return kit.getGlobalCoinMarketsAsync(currency)
    }

    override fun getGlobalCoinMarketPointsAsync(currencyCode: String, timePeriod: TimePeriod): Single<List<GlobalCoinMarketPoint>> {
        return kit.getGlobalCoinMarketPointsAsync(currencyCode, timePeriod)
    }

    override fun searchCoins(searchText: String): List<CoinData> {
        return kit.searchCoins(searchText)
    }

    override fun defiTvlPoints(coinType: CoinType, currencyCode: String, fetchDiffPeriod: TimePeriod) : Single<List<DefiTvlPoint>> {
        return kit.getDefiTvlPointsAsync(coinType.toCoinKitCoinType(), currencyCode, fetchDiffPeriod)
    }

    override fun getCoinMarketVolumePointsAsync(coinType: CoinType, currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarketPoint>> {
        return kit.getCoinMarketPointsAsync(coinType.toCoinKitCoinType(), currencyCode, fetchDiffPeriod)
    }

    override fun getCryptoNews(timestamp: Long?): Single<List<CryptoNews>> {
        return kit.cryptoNewsAsync(timestamp)
    }

}

private fun CoinType.toCoinKitCoinType(): CoinKitCoinType = when (this) {
    CoinType.Bitcoin -> CoinKitCoinType.Bitcoin
    CoinType.BitcoinCash -> CoinKitCoinType.BitcoinCash
    CoinType.Litecoin -> CoinKitCoinType.Litecoin
    CoinType.Dash -> CoinKitCoinType.Dash
    CoinType.Zcash -> CoinKitCoinType.Zcash
    CoinType.Ethereum -> CoinKitCoinType.Ethereum
    CoinType.BinanceSmartChain -> CoinKitCoinType.BinanceSmartChain
    is CoinType.Erc20 -> CoinKitCoinType.Erc20(address)
    is CoinType.Bep20 -> CoinKitCoinType.Bep20(address)
    is CoinType.Bep2 -> CoinKitCoinType.Bep2(symbol)
    is CoinType.Sol20 -> CoinKitCoinType.Unsupported("sol20:$address")
    is CoinType.Unsupported -> CoinKitCoinType.Unsupported(type)
}

fun CoinKitCoinType.toMarketKitCoinType(): CoinType = when (this) {
    CoinKitCoinType.Bitcoin -> CoinType.Bitcoin
    CoinKitCoinType.BitcoinCash -> CoinType.BitcoinCash
    CoinKitCoinType.Litecoin -> CoinType.Litecoin
    CoinKitCoinType.Dash -> CoinType.Dash
    CoinKitCoinType.Zcash -> CoinType.Zcash
    CoinKitCoinType.Ethereum -> CoinType.Ethereum
    CoinKitCoinType.BinanceSmartChain -> CoinType.BinanceSmartChain
    is CoinKitCoinType.Erc20 -> CoinType.Erc20(address)
    is CoinKitCoinType.Bep20 -> CoinType.Bep20(address)
    is CoinKitCoinType.Bep2 -> CoinType.Bep2(symbol)
    is CoinKitCoinType.Unsupported -> CoinType.Unsupported(id)
}

// todo should be replaced with real coin UID
fun CoinData.uid(): String = "uid"