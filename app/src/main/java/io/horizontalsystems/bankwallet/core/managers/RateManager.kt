package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.IRateCoinMapper
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
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
        private val rateCoinMapper: IRateCoinMapper) : IRateManager {

    private val disposables = CompositeDisposable()
    private val coinMarketCapApiKey = "f33ccd44-6545-4cbb-991c-4584b9501251"
    private val kit: XRatesKit by lazy {
        XRatesKit.create(
                context,
                currencyManager.baseCurrency.code,
                rateExpirationInterval = 60 * 10,
                topMarketsCount = 100,
                coinMarketCapApiKey = coinMarketCapApiKey
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

    override fun set(coins: List<String>) {
        val convertedCoins = coins.mapNotNull { rateCoinMapper.convert(it) }
        kit.set(convertedCoins)
    }

    override fun marketInfo(coinCode: String, currencyCode: String): MarketInfo? {
        return rateCoinMapper.convert(coinCode)?.let { kit.getMarketInfo(it, currencyCode) }
    }

    override fun getLatestRate(coinCode: String, currencyCode: String): BigDecimal? {
        val marketInfo = marketInfo(coinCode, currencyCode)

        return when {
            marketInfo == null -> null
            marketInfo.isExpired() -> null
            else -> marketInfo.rate
        }

    }

    override fun marketInfoObservable(coinCode: String, currencyCode: String): Observable<MarketInfo> {
        return rateCoinMapper.convert(coinCode)?.let { kit.marketInfoObservable(it, currencyCode) }
                ?: Observable.error(RateError.DisabledCoin())
    }

    override fun marketInfoObservable(currencyCode: String): Observable<Map<String, MarketInfo>> {
        return kit.marketInfoMapObservable(currencyCode)
                .map { marketInfo ->
                    marketInfo.map { rateCoinMapper.unconvert(it.key) to it.value }.toMap()
                }
    }

    override fun historicalRateCached(coinCode: String, currencyCode: String, timestamp: Long): BigDecimal? {
        val convertedCoinCode = rateCoinMapper.convert(coinCode) ?: return null

        return kit.historicalRate(convertedCoinCode, currencyCode, timestamp)
    }

    override fun historicalRate(coinCode: String, currencyCode: String, timestamp: Long): Single<BigDecimal> {
        val convertedCoinCode = rateCoinMapper.convert(coinCode)
                ?: return Single.error(RateError.DisabledCoin())

        return kit.historicalRate(convertedCoinCode, currencyCode, timestamp)?.let { Single.just(it) }
                ?: kit.historicalRateFromApi(convertedCoinCode, currencyCode, timestamp)
    }

    override fun chartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo? {
        return rateCoinMapper.convert(coinCode)?.let { kit.getChartInfo(it, currencyCode, chartType) }
    }

    override fun chartInfoObservable(coinCode: String, currencyCode: String, chartType: ChartType): Observable<ChartInfo> {
        return rateCoinMapper.convert(coinCode)?.let { kit.chartInfoObservable(it, currencyCode, chartType) }
                ?: Observable.error(RateError.DisabledCoin())
    }

    override fun getCryptoNews(coinCode: String): Single<List<CryptoNews>> {
        return kit.cryptoNews(coinCode)
    }

    override fun getTopMarketList(currency: String): Single<List<TopMarket>> {
        return kit.getTopMarkets(currency)
    }

    override fun refresh() {
        kit.refresh()
    }

    private fun onWalletsUpdated(wallets: List<Wallet>) {
        kit.set(wallets.mapNotNull { rateCoinMapper.convert(it.coin.code) })
    }

    private fun onBaseCurrencyUpdated() {
        kit.set(currencyManager.baseCurrency.code)
    }


    sealed class RateError : Exception() {
        class DisabledCoin : RateError()
    }

}
