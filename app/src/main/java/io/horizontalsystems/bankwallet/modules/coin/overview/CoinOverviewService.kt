package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.coin.LastPoint
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.net.URL

class CoinOverviewService(
    private val fullCoin: FullCoin,
    val currency: Currency,
    private val xRateManager: IRateManager,
    private val marketKit: MarketKit,
    private val chartTypeStorage: IChartTypeStorage,
    private val guidesBaseUrl: String,
) : Clearable {

    sealed class CoinDetailsState {
        object Loading : CoinDetailsState()
        object Loaded : CoinDetailsState()
        data class Error(val error: Throwable) : CoinDetailsState()
    }

    val coinUid get() = fullCoin.coin.uid
    var coinType: CoinType = fullCoin.platforms.first().coinType

    val coinPriceAsync = BehaviorSubject.create<CoinPrice>()
    val chartInfoUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.create()
    val chartSpinnerObservable: BehaviorSubject<Unit> = BehaviorSubject.create()
    val chartInfoErrorObservable: BehaviorSubject<Throwable> = BehaviorSubject.create()
    val coinDetailsStateObservable: BehaviorSubject<CoinDetailsState> = BehaviorSubject.createDefault(
        CoinDetailsState.Loading)
    val topTokenHoldersStateObservable: BehaviorSubject<CoinDetailsState> = BehaviorSubject.createDefault(
        CoinDetailsState.Loading)

    var coinMarketDetails: CoinMarketDetails? = null
    var topTokenHolders: List<TokenHolder> = listOf()

    var lastPoint: LastPoint? = marketKit.coinPrice(fullCoin.coin.uid, currency.code)?.let { LastPoint(it.value, it.timestamp, it.diff) }
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
        marketKit.coinPrice(fullCoin.coin.uid, currency.code)?.let {
            coinPriceAsync.onNext(it)
        }
        marketKit.coinPriceObservable(fullCoin.coin.uid, currency.code)
                .subscribeIO {
                    coinPriceAsync.onNext(it)
                }
                .let {
                    disposables.add(it)
                }
        marketKit.coinPriceObservable(fullCoin.coin.uid, currency.code)
                .subscribeIO({ marketInfo ->
                    lastPoint = LastPoint(marketInfo.value, marketInfo.timestamp, marketInfo.diff)
                }, {
                    //ignore
                }).let {
                    disposables.add(it)
                }
    }

    var chartType: ChartType
        get() = chartTypeStorage.chartType ?: ChartType.TODAY
        set(value) {
            chartTypeStorage.chartType = value
        }

    private val guideUrls = mapOf(
        "bitcoin" to "guides/token_guides/en/bitcoin.md",
        "ethereum" to "guides/token_guides/en/ethereum.md",
        "bitcoin-cash" to "guides/token_guides/en/bitcoin-cash.md",
        "zcash" to "guides/token_guides/en/zcash.md",
        "uniswap" to "guides/token_guides/en/uniswap.md",
        "curve-dao-token" to "guides/token_guides/en/curve-finance.md",
        "balancer" to "guides/token_guides/en/balancer-dex.md",
        "synthetix-network-token" to "guides/token_guides/en/synthetix.md",
        "tether" to "guides/token_guides/en/tether.md",
        "maker" to "guides/token_guides/en/makerdao.md",
        "dai" to "guides/token_guides/en/makerdao.md",
        "aave" to "guides/token_guides/en/aave.md",
        "compound" to "guides/token_guides/en/compound.md",
    )

    val guideUrl: String?
        get() = guideUrls[fullCoin.coin.uid]?.let { URL(URL(guidesBaseUrl), it).toString() }


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

    fun getTopTokenHolders(){
        xRateManager.getTopTokenHoldersAsync(coinType)
                .subscribeIO({ topTokenHolders ->
                    this.topTokenHolders = topTokenHolders
                    topTokenHoldersStateObservable.onNext(CoinDetailsState.Loaded)
                }, {
                    topTokenHoldersStateObservable.onNext(CoinDetailsState.Error(it))
                }).let {
                    disposables.add(it)
                }
    }

    fun updateChartInfo() {
        chartInfoDisposable?.dispose()

        chartInfo = xRateManager.chartInfo(coinType, currency.code, chartType)
        if (chartInfo == null){
                //show chart spinner only when chart data is not locally cached
                // and we need to wait for network response for data
            chartSpinnerObservable.onNext(Unit)
        }
        xRateManager.chartInfoObservable(coinType, currency.code, chartType)
                .subscribeIO({ chartInfo ->
                    this.chartInfo = chartInfo
                }, {
                    chartInfoErrorObservable.onNext(it)
                }).let {
                    chartInfoDisposable = it
                }
    }

    override fun clear() {
        chartInfoDisposable?.dispose()
        disposables.clear()
    }
}
