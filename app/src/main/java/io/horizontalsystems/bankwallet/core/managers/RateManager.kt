package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
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
        private val rateCoinMapper: IRateCoinMapper,
        private val feeCoinProvider: FeeCoinProvider,
        private val appConfigProvider: IAppConfigProvider) : IRateManager {

    private val disposables = CompositeDisposable()
    private val kit: XRatesKit by lazy {
        XRatesKit.create(
                context,
                currencyManager.baseCurrency.code,
                rateExpirationInterval = 60 * 10,
                topMarketsCount = 100,
                cryptoCompareApiKey = appConfigProvider.cryptoCompareApiKey,
                uniswapGraphUrl = appConfigProvider.uniswapGraphUrl
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

    private fun buildXRatesInputArgs(coins: List<io.horizontalsystems.bankwallet.entities.Coin> ): List<Coin> {

        val coinArgs = mutableListOf<Coin>()
        coins.forEach{ coin ->
            val coinType = when (coin.type){
                is io.horizontalsystems.bankwallet.entities.CoinType.Bitcoin -> CoinType.Bitcoin
                is io.horizontalsystems.bankwallet.entities.CoinType.BitcoinCash -> CoinType.BitcoinCash
                is io.horizontalsystems.bankwallet.entities.CoinType.Dash -> CoinType.Dash
                is io.horizontalsystems.bankwallet.entities.CoinType.Ethereum -> CoinType.Ethereum
                is io.horizontalsystems.bankwallet.entities.CoinType.Litecoin -> CoinType.Litecoin
                is io.horizontalsystems.bankwallet.entities.CoinType.Zcash -> CoinType.Zcash
                is io.horizontalsystems.bankwallet.entities.CoinType.Binance -> CoinType.Binance
                is io.horizontalsystems.bankwallet.entities.CoinType.Eos -> CoinType.Eos
                is io.horizontalsystems.bankwallet.entities.CoinType.Erc20 -> CoinType.Erc20(coin.type.address)
            }
            coinArgs.add(Coin(coin.code, coin.title, coinType))
        }

        return coinArgs
    }

    override fun set(coins: List<io.horizontalsystems.bankwallet.entities.Coin>) {
        kit.set(buildXRatesInputArgs(coins))
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

    override fun getTopMarketList(currency: String, fetchDiffPeriod: TimePeriod): Single<List<TopMarket>> {
        return kit.getTopMarketsAsync(currencyCode = currency, fetchDiffPeriod = fetchDiffPeriod)
    }

    override fun getTopDefiMarketList(currency: String, fetchDiffPeriod: TimePeriod): Single<List<TopMarket>> {
        return kit.getTopDefiMarketsAsync(currencyCode = currency, fetchDiffPeriod = fetchDiffPeriod)
    }

    override fun getGlobalMarketInfoAsync(currency: String): Single<GlobalMarketInfo> {
        return kit.getGlobalMarketInfoAsync(currency)
    }

    override fun refresh() {
        kit.refresh()
    }

    private fun onWalletsUpdated(wallets: List<Wallet>) {
        val feeCoins = wallets.mapNotNull { feeCoinProvider.feeCoinData(it.coin)?.first }
        val coins = wallets.map { it.coin }
        val uniqueCoins = (feeCoins + coins).distinct()
        kit.set(buildXRatesInputArgs(uniqueCoins))
    }

    private fun onBaseCurrencyUpdated() {
        kit.set(currencyManager.baseCurrency.code)
    }


    sealed class RateError : Exception() {
        class DisabledCoin : RateError()
    }

}
