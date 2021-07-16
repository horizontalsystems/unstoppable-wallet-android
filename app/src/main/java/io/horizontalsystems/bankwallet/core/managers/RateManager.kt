package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.coinkit.models.CoinType
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

    override fun latestRate(coinType: CoinType, currencyCode: String): LatestRate? {
        return kit.getLatestRate(coinType, currencyCode)
    }

    override fun latestRate(coinTypes: List<CoinType>, currencyCode: String): Map<CoinType, LatestRate> {
        return kit.getLatestRateMap(coinTypes, currencyCode)
    }

    override fun getLatestRate(coinType: CoinType, currencyCode: String): BigDecimal? {
        val marketInfo = latestRate(coinType, currencyCode)

        return when {
            marketInfo == null -> null
            marketInfo.isExpired() -> null
            else -> marketInfo.rate
        }

    }

    override fun latestRateObservable(coinType: CoinType, currencyCode: String): Observable<LatestRate> {
        return kit.getLatestRateAsync(coinType, currencyCode)
    }

    override fun latestRateObservable(coinTypes: List<CoinType>, currencyCode: String): Observable<Map<CoinType, LatestRate>> {
        return kit.latestRateMapObservable(coinTypes, currencyCode)
    }

    override fun historicalRateCached(coinType: CoinType, currencyCode: String, timestamp: Long): BigDecimal? {
        return kit.getHistoricalRate(coinType, currencyCode, timestamp)
    }

    override fun historicalRate(coinType: CoinType, currencyCode: String, timestamp: Long): Single<BigDecimal> {
        return kit.getHistoricalRate(coinType, currencyCode, timestamp)?.let { Single.just(it) }
                ?: kit.getHistoricalRateAsync(coinType, currencyCode, timestamp)
    }

    override fun chartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType): ChartInfo? {
        return kit.getChartInfo(coinType, currencyCode, chartType)
    }

    override fun chartInfoObservable(coinType: CoinType, currencyCode: String, chartType: ChartType): Observable<ChartInfo> {
        return kit.chartInfoObservable(coinType, currencyCode, chartType)
    }

    override fun coinMarketDetailsAsync(coinType: CoinType, currencyCode: String, rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>): Single<CoinMarketDetails> {
        return kit.getCoinMarketDetailsAsync(coinType, currencyCode, rateDiffCoinCodes, rateDiffPeriods)
    }

    override fun getTopTokenHoldersAsync(coinType: CoinType): Single<List<TokenHolder>> {
        return kit.getTopTokenHoldersAsync(coinType)
    }

    override fun getTopMarketList(currency: String, itemsCount: Int, diffPeriod: TimePeriod): Single<List<CoinMarket>> {
        return kit.getTopCoinMarketsAsync(currency, itemsCount = itemsCount, fetchDiffPeriod = diffPeriod)
    }

    override fun getTopDefiTvlAsync(currencyCode: String, fetchDiffPeriod: TimePeriod, itemsCount: Int, chain: String?): Single<List<DefiTvl>> {
        return kit.getTopDefiTvlAsync(currencyCode, fetchDiffPeriod, itemsCount, chain)
    }

    override fun getCoinMarketList(coinTypes: List<CoinType>, currency: String): Single<List<CoinMarket>> {
        return kit.getCoinMarketsAsync(coinTypes, currency)
    }

    override fun getCoinMarketListByCategory(categoryId: String, currency: String): Single<List<CoinMarket>>{
        return kit.getCoinMarketsByCategoryAsync(categoryId, currency)
    }

    override fun getCoinRatingsAsync(): Single<Map<CoinType, String>> {
        return kit.getCoinRatingsAsync()
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

    override fun getNotificationCoinCode(coinType: CoinType): String? {
        return kit.getNotificationCoinCode(coinType)
    }

    override fun topDefiTvl(currencyCode: String, fetchDiffPeriod: TimePeriod, itemsCount: Int) : Single<List<DefiTvl>> {
        return kit.getTopDefiTvlAsync(currencyCode, fetchDiffPeriod, itemsCount)
    }

    override fun defiTvlPoints(coinType: CoinType, currencyCode: String, fetchDiffPeriod: TimePeriod) : Single<List<DefiTvlPoint>> {
        return kit.getDefiTvlPointsAsync(coinType, currencyCode, fetchDiffPeriod)
    }

    override fun getCoinMarketVolumePointsAsync(coinType: CoinType, currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarketPoint>> {
        return kit.getCoinMarketPointsAsync(coinType, currencyCode, fetchDiffPeriod)
    }

    override fun refresh(currencyCode: String) {
        kit.refresh(currencyCode)
    }

    override fun getCryptoNews(timestamp: Long?): Single<List<CryptoNews>> {
        return kit.cryptoNewsAsync(timestamp)
    }
}
