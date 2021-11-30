package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.ILanguageManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.net.URL

class CoinOverviewService(
    val fullCoin: FullCoin,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val appConfigProvider: AppConfigProvider,
    private val languageManager: ILanguageManager,
    private val chartRepo: ChartRepo
) {
    val currency get() = currencyManager.baseCurrency

    val chartDataObservable by chartRepo::chartDataObservable

    private val coinPriceSubject = BehaviorSubject.create<DataState<CoinPrice>>()
    val coinPriceObservable: Observable<DataState<CoinPrice>>
        get() = coinPriceSubject

    private val coinOverviewSubject = BehaviorSubject.create<DataState<CoinOverviewItem>>()
    val coinOverviewObservable: Observable<DataState<CoinOverviewItem>>
        get() = coinOverviewSubject

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

    private val guideUrl: String?
        get() = guideUrls[fullCoin.coin.uid]?.let { URL(URL(appConfigProvider.guidesUrl), it).toString() }

    private val disposables = CompositeDisposable()

    fun start() {
        chartRepo.start()
        fetchCoinOverview()
        fetchCoinPrice()
    }

    fun changeChartType(chartType: ChartType) {
        chartRepo.changeChartType(chartType)
    }

    private fun fetchCoinOverview() {
        coinOverviewSubject.onNext(DataState.Loading)

        marketKit.marketInfoOverviewSingle(fullCoin.coin.uid, currencyManager.baseCurrency.code, languageManager.currentLanguage)
            .subscribeIO({ marketInfoOverview ->
                coinOverviewSubject.onNext(DataState.Success(CoinOverviewItem(fullCoin.coin.code, marketInfoOverview, guideUrl)))
            }, {
                coinOverviewSubject.onNext(DataState.Error(it))
            }).let {
                disposables.add(it)
            }
    }

    private fun fetchCoinPrice() {
        val coinPrice = marketKit.coinPrice(fullCoin.coin.uid, currency.code)
        if (coinPrice != null) {
            coinPriceSubject.onNext(DataState.Success(coinPrice))
        } else {
            coinPriceSubject.onNext(DataState.Loading)
        }

        marketKit.coinPriceObservable(fullCoin.coin.uid, currency.code)
            .subscribeIO({
                coinPriceSubject.onNext(DataState.Success(it))
            }, {
                coinPriceSubject.onNext(DataState.Error(it))
            })
            .let {
                disposables.add(it)
            }
    }

    fun stop() {
        chartRepo.stop()
        disposables.clear()
    }

    fun refresh() {
        stop()
        start()
    }
}
