package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketTicker
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class CoinMarketsService(
    val fullCoin: FullCoin,
    private val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit,
) {
    val currency get() = currencyManager.baseCurrency

    var sortType = SortType.HighestVolume
        private set

    var volumeType = VolumeType.Coin
        private set

    private val sortTypeSubject = BehaviorSubject.create<SortType>()
    val sortTypeObservable: Observable<SortType> = sortTypeSubject

    private val volumeTypeSubject = BehaviorSubject.create<VolumeType>()
    val volumeTypeObservable: Observable<VolumeType> = volumeTypeSubject

    private val itemsSubject = BehaviorSubject.create<List<MarketTickerItem>>()
    val itemsObservable: Observable<List<MarketTickerItem>> = itemsSubject

    private val disposables = CompositeDisposable()
    private var marketTickers = listOf<MarketTicker>()
    private val price = marketKit.coinPrice(fullCoin.coin.uid, currencyManager.baseCurrency.code)?.value ?: BigDecimal.ZERO

    fun start() {
        sortTypeSubject.onNext(sortType)
        volumeTypeSubject.onNext(volumeType)

        marketKit.marketTickersSingle(fullCoin.coin.uid)
            .subscribeIO {
                marketTickers = it
                emitItems()
            }
            .let {
                disposables.add(it)
            }
    }

    fun stop() {
        disposables.clear()
    }

    fun setSortType(sortType: SortType) {
        this.sortType = sortType
        sortTypeSubject.onNext(sortType)

        emitItems()
    }

    fun setVolumeType(volumeType: VolumeType) {
        this.volumeType = volumeType
        volumeTypeSubject.onNext(volumeType)

        emitItems()
    }

    @Synchronized
    private fun emitItems() {
        val sorted = when (sortType) {
            SortType.HighestVolume -> marketTickers.sortedByDescending { it.volume }
            SortType.LowestVolume -> marketTickers.sortedBy { it.volume }
        }

        itemsSubject.onNext(sorted.map { createItem(it) })
    }

    private fun createItem(marketTicker: MarketTicker): MarketTickerItem {
        val volume = when (volumeType) {
            VolumeType.Coin -> marketTicker.volume
            VolumeType.Currency -> marketTicker.volume.multiply(price)
        }

        return MarketTickerItem(
            marketTicker.marketName,
            marketTicker.marketImageUrl,
            marketTicker.base,
            marketTicker.target,
            marketTicker.rate,
            volume,
            volumeType
        )
    }
}
