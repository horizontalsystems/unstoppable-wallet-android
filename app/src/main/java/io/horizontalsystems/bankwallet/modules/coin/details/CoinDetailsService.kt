package io.horizontalsystems.bankwallet.modules.coin.details

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinDetailsService(
    private val fullCoin: FullCoin,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager
) {
    private val disposables = CompositeDisposable()

    private val stateSubject = BehaviorSubject.create<DataState<Item>>()
    val stateObservable: Observable<DataState<Item>>
        get() = stateSubject

    val usdCurrency: Currency
        get() {
            val currencies = currencyManager.currencies
            return currencies.first { it.code == "USD" }
        }

    val currency: Currency
        get() = currencyManager.baseCurrency

    val hasMajorHolders: Boolean by lazy { fullCoin.platforms.any { it.coinType is CoinType.Erc20 } }

    val auditAddresses: List<String> by lazy {
        fullCoin.platforms.mapNotNull {
            when (val coinType = it.coinType) {
                is CoinType.Erc20 -> coinType.address
                is CoinType.Bep20 -> coinType.address
                else -> null
            }
        }
    }

    val coin = fullCoin.coin

    private fun fetchCharts(details: MarketInfoDetails): Single<Item> {
        val tvlsSingle: Single<List<ChartPoint>> = if (details.tvl != null) {
            marketKit.marketInfoTvlSingle(fullCoin.coin.uid, currency.code, TimePeriod.Day30)
        } else {
            Single.just(listOf())
        }

        val volumeSingle = marketKit.chartInfoSingle(fullCoin.coin.uid, currency.code, ChartType.MONTHLY_BY_DAY)
            .map { chartInfo ->
                chartInfo.points.mapNotNull { point ->
                    point.volume?.let {
                        ChartPoint(it, it, point.timestamp)
                    }
                }
            }

        return Single.zip(
            tvlsSingle.onErrorReturn { listOf() },
            volumeSingle.onErrorReturn { listOf() },
            { t1, t2 -> Pair(t1, t2) }
        ).map { (tvls, totalVolumes) ->
            Item(details, tvls, totalVolumes)
        }
    }

    private fun fetch() {
        marketKit.marketInfoDetailsSingle(fullCoin.coin.uid, currency.code)
            .doOnSubscribe { stateSubject.onNext(DataState.Loading) }
            .flatMap { details ->
                fetchCharts(details)
            }
            .subscribeIO({ item ->
                stateSubject.onNext(DataState.Success(item))
            }, {
                stateSubject.onNext(DataState.Error(it))
            }).let {
                disposables.add(it)
            }
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposables.clear()
    }

    data class Item(
        val marketInfoDetails: MarketInfoDetails,
        val tvls: List<ChartPoint>?,
        val totalVolumes: List<ChartPoint>?
    )

}
