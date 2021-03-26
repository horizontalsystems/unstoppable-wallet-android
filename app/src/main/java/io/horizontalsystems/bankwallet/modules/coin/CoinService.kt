package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.net.URL

class CoinService(
        val coinType: CoinType,
        val currency: Currency,
        private val xRateManager: IRateManager,
        private val chartTypeStorage: IChartTypeStorage,
        private val priceAlertManager: IPriceAlertManager,
        private val notificationManager: INotificationManager,
        private val localStorage: ILocalStorage,
        private val marketFavoritesManager: MarketFavoritesManager,
        guidesBaseUrl: String
) : Clearable {

    sealed class CoinDetailsState {
        object Loading : CoinDetailsState()
        object Loaded : CoinDetailsState()
        data class Error(val error: Throwable) : CoinDetailsState()
    }

    val latestRateAsync = BehaviorSubject.create<LatestRate>()
    val chartInfoUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.create()
    val chartInfoErrorObservable: BehaviorSubject<Throwable> = BehaviorSubject.create()
    val coinDetailsStateObservable: BehaviorSubject<CoinDetailsState> = BehaviorSubject.createDefault(CoinDetailsState.Loading)
    val alertNotificationUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.createDefault(Unit)
    val notificationSupported: Boolean
        get() {
            return priceAlertManager.notificationCode(coinType) != null
        }

    val hasPriceAlert: Boolean
        get() {
            return priceAlertManager.hasPriceAlert(coinType)
        }

    var coinMarketDetails: CoinMarketDetails? = null

    var lastPoint: LastPoint? = xRateManager.latestRate(coinType, currency.code)?.let { LastPoint(it.rate, it.timestamp, it.rateDiff24h) }
        set(value) {
            field = value
            triggerChartUpdateIfEnoughData()
        }

    var chartInfo: ChartInfo? = null
        set(value) {
            field = value
            triggerChartUpdateIfEnoughData()
        }

    private fun triggerChartUpdateIfEnoughData() {
        if (chartInfo != null && lastPoint != null) {
            chartInfoUpdatedObservable.onNext(Unit)
        }
    }

    private val disposables = CompositeDisposable()
    private var chartInfoDisposable: Disposable? = null

    init {
        priceAlertManager.notificationChangedFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    alertNotificationUpdatedObservable.onNext(Unit)
                }
                .let {
                    disposables.add(it)
                }

        xRateManager.latestRate(coinType, currency.code)?.let {
            latestRateAsync.onNext(it)
        }
        xRateManager.latestRateObservable(coinType, currency.code)
                .subscribeIO {
                    latestRateAsync.onNext(it)
                }
                .let {
                    disposables.add(it)
                }
        xRateManager.latestRateObservable(coinType, currency.code)
                .subscribeIO({ marketInfo ->
                    lastPoint = LastPoint(marketInfo.rate, marketInfo.timestamp, marketInfo.rateDiff24h)
                }, {
                    //ignore
                }).let {
                    disposables.add(it)
                }
    }

    val notificationsAreEnabled: Boolean
        get() = notificationManager.isEnabled && localStorage.isAlertNotificationOn

    var chartType: ChartType
        get() = chartTypeStorage.chartType ?: ChartType.TODAY
        set(value) {
            chartTypeStorage.chartType = value
        }

    private val guidesBaseUrl = URL(guidesBaseUrl)

    val guideUrl: String?
        get() {
            val guideRelativeUrl = when (coinType) {
                CoinType.Bitcoin -> "guides/token_guides/en/bitcoin.md"
                CoinType.Ethereum -> "guides/token_guides/en/ethereum.md"
                CoinType.BitcoinCash -> "guides/token_guides/en/bitcoin-cash.md"
                CoinType.Zcash -> "guides/token_guides/en/zcash.md"
                is CoinType.Erc20 -> {
                    when (coinType.address) {
                        "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984" -> "guides/token_guides/en/uniswap.md"
                        "0xd533a949740bb3306d119cc777fa900ba034cd52" -> "guides/token_guides/en/curve-finance.md"
                        "0xba100000625a3754423978a60c9317c58a424e3d" -> "guides/token_guides/en/balancer-dex.md"
                        "0xc011a73ee8576fb46f5e1c5751ca3b9fe0af2a6f" -> "guides/token_guides/en/synthetix.md"
                        "0xdac17f958d2ee523a2206206994597c13d831ec7" -> "guides/token_guides/en/tether.md"
                        "0x9f8f72aa9304c8b593d555f12ef6589cc3a579a2" -> "guides/token_guides/en/makerdao.md"
                        "0x6b175474e89094c44da98b954eedeac495271d0f" -> "guides/token_guides/en/makerdao.md"
                        "0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9" -> "guides/token_guides/en/aave.md"
                        "0xc00e94cb662c3520282e6f5717214004a7f26888" -> "guides/token_guides/en/compound.md"
                        else -> null
                    }
                }
                else -> null
            }
            return guideRelativeUrl?.let {
                URL(guidesBaseUrl, it).toString()
            }
        }

    fun getCoinDetails(rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>) {
        coinDetailsStateObservable.onNext(CoinDetailsState.Loading)
        xRateManager.coinMarketDetailsAsync(coinType, currency.code, rateDiffCoinCodes, rateDiffPeriods)
                .subscribeIO({ coinMarketDetails ->
                    this.coinMarketDetails = coinMarketDetails
                    coinDetailsStateObservable.onNext(CoinDetailsState.Loaded)
                }, {
                    coinDetailsStateObservable.onNext(CoinDetailsState.Error(it))
                }).let {
                    disposables.add(it)
                }
    }

    fun updateChartInfo() {
        chartInfoDisposable?.dispose()

        chartInfo = xRateManager.chartInfo(coinType, currency.code, chartType)
        xRateManager.chartInfoObservable(coinType, currency.code, chartType)
                .subscribeIO({ chartInfo ->
                    this.chartInfo = chartInfo
                }, {
                    chartInfoErrorObservable.onNext(it)
                }).let {
                    chartInfoDisposable = it
                }
    }

    fun isCoinFavorite(): Boolean {
        return marketFavoritesManager.isCoinInFavorites(coinType)
    }

    fun favorite() {
        marketFavoritesManager.add(coinType)
    }

    fun unfavorite() {
        marketFavoritesManager.remove(coinType)
    }

    override fun clear() {
        chartInfoDisposable?.dispose()
        disposables.clear()
    }
}
