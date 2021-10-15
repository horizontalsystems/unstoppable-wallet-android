package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListFetcher
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.MarketInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import io.horizontalsystems.xrateskit.entities.TimePeriod as XRatesTimePeriod


class MarketAdvancedSearchService(
        private val marketKit: MarketKit,
        private val currencyManager: ICurrencyManager
) : Clearable, IMarketListFetcher {

    private val allTimeDeltaPercent = BigDecimal.TEN

    var coinCount: Int = CoinList.Top250.itemsCount
        set(value) {
            field = value
            cache = null

            refreshCounter()
        }
    var filterMarketCap: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterVolume: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterLiquidity: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterPeriod: XRatesTimePeriod = XRatesTimePeriod.HOUR_24
        set(value) {
            field = value
            cache = null

            refreshCounter()
        }
    var filterPriceChange: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterOutperformedBtcOn: Boolean = false
        set(value) {
            field = value

            refreshCounter()
        }
    var filterOutperformedEthOn: Boolean = false
        set(value) {
            field = value

            refreshCounter()
        }
    var filterOutperformedBnbOn: Boolean = false
        set(value) {
            field = value

            refreshCounter()
        }
    var filterPriceCloseToAth: Boolean = false
        set(value) {
            field = value

            refreshCounter()
        }
    var filterPriceCloseToAtl: Boolean = false
        set(value) {
            field = value

            refreshCounter()
        }

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    var numberOfItemsAsync = BehaviorSubject.create<DataState<Int>>()

    private var topItemsDisposable: Disposable? = null
    private var disposables = CompositeDisposable()
    private var cache: List<MarketInfo>? = null

    init {
        refreshCounter()

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeIO {
                    cache = null
                    refreshCounter()
                }
                .let {
                    disposables.add(it)
                }
    }

    private fun refreshCounter() {
        topItemsDisposable?.dispose()

        numberOfItemsAsync.onNext(DataState.Loading)
        topItemsDisposable = getTopMarketList(currencyManager.baseCurrency)
                .map { it.size }
                .subscribeIO({
                    numberOfItemsAsync.onNext(DataState.Success(it))
                }, {
                    numberOfItemsAsync.onNext(DataState.Error(it))
                })

    }

    override fun fetchAsync(currency: Currency): Single<List<MarketItem>> {
        return getTopMarketList(currency)
                .map { coinMarkets ->
                    coinMarkets.map {
                        val index = it.key
                        val coinMarket = it.value

                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rank(index + 1))
                    }
                }
    }

    private fun getTopMarketList(currency: Currency): Single<Map<Int, MarketInfo>> {
        val topMarketListAsync = if (cache != null) {
            Single.just(cache)
        } else {
            // todo: add support for currency and filterPeriod
            marketKit.marketInfosSingle(coinCount)
                    .doOnSuccess {
                        cache = it
                    }
        }

        return topMarketListAsync
                .map {
                    it.mapIndexed { index, coinMarket ->
                        index to coinMarket
                    }.filter {
                        filterCoinMarket(it.second)
                    }.toMap()
                }
    }

    private fun filterCoinMarket(marketInfo: MarketInfo): Boolean {
        val btcUid = App.coinManager.getPlatformCoin(CoinType.Bitcoin)!!.coin.uid
        val ethUid = App.coinManager.getPlatformCoin(CoinType.Ethereum)!!.coin.uid
        val bnbUid = App.coinManager.getPlatformCoin(CoinType.Bep2("BNB"))!!.coin.uid

        return filterByRange(filterMarketCap, marketInfo.marketCap?.toLong())
                && filterByRange(filterVolume, marketInfo.totalVolume?.toLong())
                && filterByRange(filterPriceChange, marketInfo.priceChange?.toLong())
//                todo: implement it
//                && (!filterPriceCloseToAth || closeToAllTime(marketInfo.athChangePercentage))
//                && (!filterPriceCloseToAtl || closeToAllTime(marketInfo.atlChangePercentage))
                && (!filterOutperformedBtcOn || outperformed(marketInfo.priceChange, btcUid))
                && (!filterOutperformedEthOn || outperformed(marketInfo.priceChange, ethUid))
                && (!filterOutperformedBnbOn || outperformed(marketInfo.priceChange, bnbUid))
    }

    private fun filterByRange(filter: Pair<Long?, Long?>?, value: Long?): Boolean {
        if (filter == null) return true

        filter.first?.let { min ->
            if (value == null || value < min) {
                return false
            }
        }

        filter.second?.let { max ->
            if (value == null || value > max) {
                return false
            }
        }

        return true
    }

    private fun marketInfo(coinUid: String): MarketInfo? = cache?.firstOrNull { it.fullCoin.coin.uid == coinUid }

    private fun outperformed(value: BigDecimal?, coinUid: String): Boolean {
        if (value == null) return false
        val coinMarket = marketInfo(coinUid) ?: return false

        return coinMarket.priceChange ?: BigDecimal.ZERO < value
    }

    private fun closeToAllTime(value: BigDecimal?): Boolean {
        value ?: return false

        return value.abs() < allTimeDeltaPercent
    }

    override fun clear() {
        topItemsDisposable?.dispose()
        disposables.dispose()
    }
}
