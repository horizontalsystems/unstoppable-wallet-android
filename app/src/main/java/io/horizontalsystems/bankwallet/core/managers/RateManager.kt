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
    private val coinMarketCapApiKey = "51a3a136-adc9-4e38-8fc2-8c175c810e74"
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

        initMapper()
    }

    override fun set(coins: List<String>) {
        val convertedCoins = coins.map { converted(it) }
        kit.set(convertedCoins)
    }

    override fun marketInfo(coinCode: String, currencyCode: String): MarketInfo? {
        return kit.getMarketInfo(converted(coinCode), currencyCode)
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
        return kit.marketInfoObservable(converted(coinCode), currencyCode)
    }

    override fun marketInfoObservable(currencyCode: String): Observable<Map<String, MarketInfo>> {
        return kit.marketInfoMapObservable(currencyCode)
                .map { marketInfo ->
                    marketInfo.map { unconverted(it.key) to it.value }.toMap()
                }
    }

    override fun historicalRate(coinCode: String, currencyCode: String, timestamp: Long): Single<BigDecimal> {
        kit.historicalRate(converted(coinCode), currencyCode, timestamp)?.let {
            return Single.just(it)
        }

        return kit.historicalRateFromApi(converted(coinCode), currencyCode, timestamp)
    }

    override fun chartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo? {
        return kit.getChartInfo(converted(coinCode), currencyCode, chartType)
    }

    override fun chartInfoObservable(coinCode: String, currencyCode: String, chartType: ChartType): Observable<ChartInfo> {
        return kit.chartInfoObservable(converted(coinCode), currencyCode, chartType)
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
        kit.set(wallets.map { converted(it.coin.code) })
    }

    private fun onBaseCurrencyUpdated() {
        kit.set(currencyManager.baseCurrency.code)
    }

    private fun converted(coinCode: String): String {
        return rateCoinMapper.convertedCoinMap[coinCode] ?: coinCode
    }

    private fun unconverted(coinCode: String): String {
        return rateCoinMapper.unconvertedCoinMap[coinCode] ?: coinCode
    }

    private fun initMapper() {
        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "HOT", to = "HOLO")
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "HOLO", to = "HOT")

        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "SAI", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "PGL", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "PPT", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "EOSDT", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "WBTC", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Convert, from = "WETH", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "SAI", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "PGL", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "PPT", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "EOSDT", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "WBTC", to = null)
        rateCoinMapper.addCoin(RateDirectionMap.Unconvert, from = "WETH", to = null)
    }

}
