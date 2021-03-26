package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.XRatesKit
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class RateManager(
        context: Context,
        walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        private val feeCoinProvider: FeeCoinProvider,
        private val appConfigProvider: IAppConfigProvider) : IRateManager {

    private val disposables = CompositeDisposable()
    private val kit: XRatesKit by lazy {
        XRatesKit.create(
                context,
                currencyManager.baseCurrency.code,
                rateExpirationInterval = 60 * 10,
                cryptoCompareApiKey = appConfigProvider.cryptoCompareApiKey
        )
    }

    init {
        walletManager.walletsUpdatedObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { wallets ->
                    onWalletsUpdated(wallets)
                }.let {
                    disposables.add(it)
                }

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onBaseCurrencyUpdated()
                }.let {
                    disposables.add(it)
                }
    }

    override fun set(coins: List<Coin>) {
        kit.set(coins.map { it.type })
    }

    override fun latestRate(coinType: CoinType, currencyCode: String): LatestRate? {
        return kit.getLatestRate(coinType, currencyCode)
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

    override fun latestRateObservable(currencyCode: String): Observable<Map<CoinType, LatestRate>> {
        return kit.latestRateMapObservable(currencyCode)
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

    override fun getCryptoNews(coinCode: String): Single<List<CryptoNews>> {
        return kit.cryptoNews(coinCode)
    }

    override fun getTopMarketList(currency: String, itemsCount: Int, diffPeriod: TimePeriod): Single<List<CoinMarket>> {
        return kit.getTopCoinMarketsAsync(currency, itemsCount = itemsCount, fetchDiffPeriod = diffPeriod)
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

    override fun searchCoins(searchText: String): List<CoinData> {
        return kit.searchCoins(searchText)
    }

    override fun getNotificationCoinCode(coinType: CoinType): String? {
        return kit.getNotificationCoinCode(coinType)
    }

    override fun refresh() {
        kit.refresh()
    }

    private fun onWalletsUpdated(wallets: List<Wallet>) {
        val feeCoins = wallets.mapNotNull { feeCoinProvider.feeCoinData(it.coin)?.first }
        val coins = wallets.map { it.coin }
        val uniqueCoins = (feeCoins + coins).distinct()
        kit.set(uniqueCoins.map { it.type })
    }

    private fun onBaseCurrencyUpdated() {
        kit.set(currencyManager.baseCurrency.code)
    }

}
